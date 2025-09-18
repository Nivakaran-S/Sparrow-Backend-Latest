FROM python:3.11-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8096

HEALTHCHECK --interval=20s --timeout=3s --retries=5 --start-period=20s \
    CMD curl -f http://localhost:8080/health || exit 1

# Use Gunicorn for production
CMD ["gunicorn", "--bind", "0.0.0.0:8080", "app:app", "--workers", "4", "--threads", "2"]