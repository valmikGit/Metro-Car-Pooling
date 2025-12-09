#!/bin/bash
# 
# Metro Car-Pooling - Minikube Setup Script
# 
# Starts Minikube with all required addons:
# - Ingress (Nginx Ingress Controller)
# - Metrics Server (Resource monitoring)
# - Dashboard (Kubernetes Dashboard UI)
#
# Usage: ./start_minikube.sh [memory] [cpus] [driver]
# Example: ./start_minikube.sh 8192 4 docker

MEMORY=${1:-4096}
CPUS=${2:-2}
DRIVER=${3:-docker}

echo "============================================"
echo "  Metro Car-Pooling - Minikube Setup"
echo "============================================"
echo ""

# Check if Minikube is installed
if ! command -v minikube &> /dev/null; then
    echo "ERROR: Minikube is not installed!"
    echo "Install using: curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64"
    echo "               sudo install minikube-linux-amd64 /usr/local/bin/minikube"
    exit 1
fi

# Check if kubectl is installed
if ! command -v kubectl &> /dev/null; then
    echo "ERROR: kubectl is not installed!"
    echo "Install using: sudo snap install kubectl --classic"
    exit 1
fi

# Check Minikube status
MINIKUBE_STATUS=$(minikube status --format='{{.Host}}' 2>/dev/null)
if [ "$MINIKUBE_STATUS" == "Running" ]; then
    echo "Minikube is already running!"
else
    echo "Starting Minikube..."
    echo "  Driver: $DRIVER"
    echo "  Memory: ${MEMORY}MB"
    echo "  CPUs: $CPUS"
    echo ""
    
    minikube start --driver=$DRIVER --memory=$MEMORY --cpus=$CPUS
    
    if [ $? -ne 0 ]; then
        echo "ERROR: Failed to start Minikube!"
        exit 1
    fi
    
    echo "Minikube started successfully!"
fi

echo ""
echo "Enabling addons..."

# Enable Ingress
echo "  Enabling ingress..."
minikube addons enable ingress
echo "  [OK] Ingress enabled"

# Enable Metrics Server
echo "  Enabling metrics-server..."
minikube addons enable metrics-server
echo "  [OK] Metrics Server enabled"

# Enable Dashboard
echo "  Enabling dashboard..."
minikube addons enable dashboard
echo "  [OK] Dashboard enabled"

echo ""
echo "============================================"
echo "  Setup Complete!"
echo "============================================"
echo ""

# Get Minikube IP
MINIKUBE_IP=$(minikube ip)
echo "Minikube IP: $MINIKUBE_IP"
echo ""

# Show kubectl context
echo "Kubernetes Context:"
kubectl config current-context
echo ""

# Deploy namespace
echo "Deploying namespace..."
kubectl apply -f k8s/namespace.yaml

echo ""
echo "============================================"
echo "  Next Steps:"
echo "============================================"
echo ""
echo "1. Add to /etc/hosts (run as root):"
echo "   echo '$MINIKUBE_IP metrocar.local' >> /etc/hosts"
echo ""
echo "2. Deploy infrastructure:"
echo "   kubectl apply -f k8s/redis/"
echo "   kubectl apply -f k8s/postgres/"
echo "   kubectl apply -f k8s/kafka/"
echo "   kubectl apply -f k8s/zookeeper/"
echo ""
echo "3. Open dashboard:"
echo "   minikube dashboard"
echo ""
echo "4. Start tunnel (for LoadBalancer services):"
echo "   minikube tunnel"
echo ""
echo "5. Access services:"
echo "   Frontend: http://metrocar.local"
echo "   API:      http://metrocar.local/api"
echo ""
