FROM python:3.12-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy proto and generate Python gRPC stubs
COPY proto/ ./proto/
RUN mkdir -p ./app/grpc_client/generated && \
    touch ./app/__init__.py && \
    touch ./app/grpc_client/__init__.py && \
    touch ./app/grpc_client/generated/__init__.py && \
    python -m grpc_tools.protoc \
      -I./proto \
      --python_out=./app/grpc_client/generated \
      --grpc_python_out=./app/grpc_client/generated \
      ./proto/billing_service.proto

# Copy application code
COPY app/ ./app/

EXPOSE 4003

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "4003"]
