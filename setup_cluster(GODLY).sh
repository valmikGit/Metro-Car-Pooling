#!/bin/bash
set -e

echo "🚀 Starting Metro Car Pool Cluster Setup..."

# 1. Reset Cluster
echo "🛑 Deleting existing Minikube cluster..."
minikube delete

echo "🔥 Starting Minikube (8GB RAM, 4 CPUs)..."
minikube start --memory=8192 --cpus=4 --driver=docker

echo "🔌 Enabling Ingress Addon..."
minikube addons enable ingress

# 2. Namespace
echo "📂 Creating Namespace 'metro'..."
kubectl create namespace metro
kubectl config set-context --current --namespace=metro

# 3. Install Operators
echo "🏗️ Installing Strimzi Kafka Operator..."
kubectl create -f 'https://strimzi.io/install/latest?namespace=metro' -n metro
# Wait for Strimzi to be ready (CRDs)
echo "⏳ Waiting for Strimzi Operator..."
kubectl wait --for=condition=Ready pod -l name=strimzi-cluster-operator -n metro --timeout=300s

echo "🐘 Installing Zalando Postgres Operator (via Helm)..."
# Add repo if not exists
helm repo add postgres-operator-charts https://opensource.zalando.com/postgres-operator/charts/postgres-operator/ || true
helm repo update
helm install postgres-operator postgres-operator-charts/postgres-operator -n metro
echo "⏳ Waiting for Postgres Operator..."
# Start pod might take a moment, wait for deployment
kubectl rollout status deployment/postgres-operator -n metro --timeout=300s

# 4. Load Images (Crucial Step)
echo "📦 Loading Docker Images into Minikube (This may take some time)..."
IMAGES=(
    "driver-driver-service:latest"
    "gateway-gateway:latest"
    "matching-matching-service:latest"
    "notification-notification-service:latest"
    "rider-rider-service:latest"
    "trip-trip-service:latest"
    "user-user-service:latest"
    "registry-service:latest"
    "frontend-frontend-service:latest" 
)

for img in "${IMAGES[@]}"; do
    echo "   Loading $img..."
    if minikube image load "$img"; then
        echo "   ✅ Loaded $img"
    else
        echo "   ⚠️ Failed to load $img (Does it exist locally?)"
    fi
done

# 5. Apply Infrastructure
echo "🏗️ Applying Infrastructure Manifests..."
# Apply Kafka Cluster
kubectl apply -f k8s/kafka/metro-kafka.yml
# Apply Postgres Cluster
kubectl apply -f k8s/postgres/postgresql-zalando.yaml
# Apply Redis
kubectl apply -f k8s/redis/redis-deployment.yml
kubectl apply -f k8s/redis/redis-service.yml
# Apply Registry
kubectl apply -f k8s/registry/registry-deployment.yml
kubectl apply -f k8s/registry/registry-service.yml

# Wait for Infra to be ready
echo "⏳ Waiting for Infrastructure (Kafka, Postgres, Registry) to stabilize..."
echo "   (This prevents crash loops in dependent services)"
sleep 10 # Give operators a moment to create pods
kubectl wait --for=condition=Ready pod -l strimzi.io/name=metro-kafka-kafka -n metro --timeout=300s || echo "Kafka not ready yet, proceeding..."
kubectl wait --for=condition=Running pod -l app.kubernetes.io/name=postgres -n metro --timeout=300s || echo "Postgres not ready yet, proceeding..."

# 6. Apply Services
echo "🚀 Applying Microservices..."
kubectl apply -f k8s/elasticsearch/elasticsearch-deployment.yaml
kubectl apply -f k8s/elasticsearch/elasticsearch-service.yaml
kubectl apply -f k8s/logging/logstash-configmap.yml
kubectl apply -f k8s/logging/logstash-deployment.yml
kubectl apply -f k8s/logging/logstash-service.yml

kubectl apply -f k8s/driver-deployment.yaml
kubectl apply -f k8s/gateway-deployment.yaml
kubectl apply -f k8s/matching-deployment.yaml
kubectl apply -f k8s/notification-deployment.yaml
kubectl apply -f k8s/rider-deployment.yaml
kubectl apply -f k8s/trip-deployment.yaml
kubectl apply -f k8s/user/user-deployment.yml
kubectl apply -f k8s/user/user-service.yml
kubectl apply -f k8s/frontend-deployment.yaml

# 7. Ingress
echo "🌐 Applying Ingress..."
kubectl apply -f k8s/ingress.yaml

echo "🎉 Setup Complete! Run 'kubectl -n metro get pods' to check status."
