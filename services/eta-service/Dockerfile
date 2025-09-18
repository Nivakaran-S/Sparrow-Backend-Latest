# ====== Base image ======
FROM python:3.11-slim

WORKDIR /app

# Prevent Python from writing pyc files & use unbuffered logs
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential curl \
    && rm -rf /var/lib/apt/lists/*

# Copy dependency file and install if it exists
COPY requirements.txt* ./
RUN if [ -f requirements.txt ]; then pip install --default-timeout=100 --no-cache-dir -r requirements.txt; fi

# Copy application code
COPY . .

# Expose port
EXPOSE 8080

# Healthcheck endpoint
HEALTHCHECK --interval=20s --timeout=3s --retries=5 --start-period=20s \
    CMD curl -f http://localhost:8080/health || exit 1

# Run the Flask application
CMD ["python3", "app.py"]