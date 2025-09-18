from langchain_core.messages import SystemMessage, HumanMessage
from src.llms.groqllm import GroqLLM
from src.states.masterState import MasterState, ExecutorState
from src.nodes.actionNode import ExecutorNode
import asyncio
from typing import List, Dict
from src.utils.prompts import master_agent_prompt
from src.states.masterState import PlannerOutput
from langgraph.constants import Send
from src.graphs.actionGraph import graph

class MasterOrchestrator:
    def __init__(self, llm):
        self.llm = llm
        self.master_planner = llm.with_structured_output(PlannerOutput)
        self.compiled_worker_graph = graph

    def classify_execution_job(self, job_description: str) -> str:
        """Map job description to specific action identifier"""
        job_lower = job_description.lower()
        
        if 'track' in job_lower and ('package' in job_lower or 'parcel' in job_lower):
            return 'track_package'
        elif 'weather' in job_lower:
            return 'get_weather'
        elif 'search' in job_lower or 'find' in job_lower:
            return 'web_search'
        # Add more action mappings as needed
        else:
            return 'general_query'

    def orchestrator(self, state: MasterState):
        """Generate a plan by breaking down the query into execution jobs"""
        
        system_prompt = """You are a master task planner. Given a query, break it down into specific, actionable execution jobs.
        
        Each job should be:
        1. Clear and specific
        2. Actionable by a specialized worker
        3. Independent or clearly sequenced
        4. Focused on a single objective
        
        Return a list of execution jobs as strings."""
        
        planner_result = self.master_planner.invoke([
            SystemMessage(content=system_prompt),
            HumanMessage(content=f"Here is the query brief: {state['query_brief']}")
        ])

        print("Execution Jobs Generated:", planner_result.executor_jobs)
        return {"execution_jobs": planner_result.executor_jobs}

    def worker_executor(self, worker_input: dict):
        """Execute a single job using the worker graph"""
        
        job_description = worker_input["execution_job"]
        action_type = self.classify_execution_job(job_description)
        
        # Prepare the initial state for the worker
        worker_state = {
            "executor_messages": [HumanMessage(content=job_description)],
            "execution_job": action_type,  # This should be 'track_package', 'get_weather', etc.
            "executor_data": []
        }
        
        print(f"Executing job: {job_description} -> Action: {action_type}")
        
        # Execute the worker graph
        try:
            result = self.compiled_worker_graph.invoke(worker_state)
            
            # Return the completed job info
            return {
                "completed_jobs": [f"Job: {job_description} - Action: {action_type} - Status: Completed"],
                "worker_outputs": [result]
            }
        except Exception as e:
            error_result = {
                "output": f"Error executing job: {str(e)}",
                "executor_data": [f"Error: {str(e)}"],
                "executor_messages": []
            }
            return {
                "completed_jobs": [f"Job: {job_description} - Action: {action_type} - Status: Failed - {str(e)}"],
                "worker_outputs": [error_result]
            }

    def assign_workers(self, state: MasterState):
        """Assign a worker to each execution job using Send"""
        return [
            Send("worker_executor", {"execution_job": job}) 
            for job in state["execution_jobs"]
        ]

    def synthesizer(self, state: MasterState):
        """Combine all completed jobs into a final output"""
        
        # Create a synthesis prompt
        synthesis_prompt = f"""
        Original Query: {state['query_brief']}
        
        Completed Jobs Summary:
        {chr(10).join([f"- {job}" for job in state['completed_jobs']])}
        
        Detailed Worker Outputs:
        {chr(10).join([f"Output {i+1}: {output.get('output', 'No output')}" for i, output in enumerate(state['worker_outputs'])])}
        
        Please synthesize all the work into a comprehensive final response that addresses the original query.
        """
        
        synthesis_result = self.llm.invoke([
            SystemMessage(content="You are a synthesis expert. Combine the worker outputs into a coherent final response."),
            HumanMessage(content=synthesis_prompt)
        ])
        
        return {"final_output": synthesis_result.content}