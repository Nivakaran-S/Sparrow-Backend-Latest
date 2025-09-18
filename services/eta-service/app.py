import os
from flask import Flask, jsonify

app = Flask(__name__)

# Example health endpoint
@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "UP"}), 200

# Example root endpoint
@app.route("/", methods=["GET"])
def root():
    return jsonify({"message": "ETA service is running"}), 200

if __name__ == "__main__":
    # Get port from environment variable, default to 8080
    port = int(os.environ.get("PORT", 8080))
    # Listen on all interfaces (0.0.0.0) for Docker
    app.run(host="0.0.0.0", port=port, debug=False)