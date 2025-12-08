#!/bin/bash

# Define the root directory (current directory)
ROOT_DIR=$(pwd)

echo "Starting restart of all services..."

# 1. Bring down all services
echo "Stopping all services..."

# List of directories known to contain docker-compose.yml
# We can find them dynamically or hardcode them based on the previous investigation.
# Dynamic finding is more robust.
SERVICES=$(find . -name "docker-compose.yml" -not -path "*/node_modules/*")

for compose_file in $SERVICES; do
    service_dir=$(dirname "$compose_file")
    echo "Stopping service in $service_dir"
    (cd "$service_dir" && docker compose down -v)
done

echo "All services stopped."

# 2. Start infra service first (priority)
if [ -d "infra" ]; then
    echo "Starting infra service..."
    (cd "infra" && docker compose up -d)
    
    # Optional: Wait for infra to be ready if needed, but for now we just start it.
    echo "Infra service started. Waiting for 15 seconds to allow initialization..."
    sleep 15
else
    echo "Error: infra directory not found!"
    exit 1
fi

# 3. Start all other services
echo "Starting other services..."

for compose_file in $SERVICES; do
    service_dir=$(dirname "$compose_file")
    
    # Skip infra as it's already started
    if [[ "$service_dir" == "./infra" || "$service_dir" == "infra" ]]; then
        continue
    fi
    
    echo "Starting service in $service_dir"
    (cd "$service_dir" && docker compose up -d)
done

echo "All services restart sequence initiated."
docker ps
