param(
    [int]$Memory = 4096,
    [int]$CPUs = 2,
    [string]$Driver = "docker"
)

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Metro Car-Pooling - Minikube Setup" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Check if Minikube is installed
if (!(Get-Command minikube -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: Minikube is not installed!" -ForegroundColor Red
    Write-Host "Install using: choco install minikube" -ForegroundColor Yellow
    Write-Host "Or: winget install Kubernetes.minikube" -ForegroundColor Yellow
    exit 1
}

# Check if kubectl is installed
if (!(Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: kubectl is not installed!" -ForegroundColor Red
    Write-Host "Install using: choco install kubernetes-cli" -ForegroundColor Yellow
    exit 1
}

# Check Minikube status
$minikubeStatus = minikube status --format='{{.Host}}' 2>$null
if ($minikubeStatus -eq "Running") {
    Write-Host "Minikube is already running!" -ForegroundColor Green
} else {
    Write-Host "Starting Minikube..." -ForegroundColor Yellow
    Write-Host "  Driver: $Driver" -ForegroundColor Gray
    Write-Host "  Memory: ${Memory}MB" -ForegroundColor Gray
    Write-Host "  CPUs: $CPUs" -ForegroundColor Gray
    Write-Host ""
    
    minikube start --driver=$Driver --memory=$Memory --cpus=$CPUs
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Failed to start Minikube!" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Minikube started successfully!" -ForegroundColor Green
}

Write-Host ""
Write-Host "Enabling addons..." -ForegroundColor Yellow

# Enable Ingress
Write-Host "  Enabling ingress..." -ForegroundColor Gray
minikube addons enable ingress
if ($LASTEXITCODE -eq 0) {
    Write-Host "  [OK] Ingress enabled" -ForegroundColor Green
}

# Enable Metrics Server
Write-Host "  Enabling metrics-server..." -ForegroundColor Gray
minikube addons enable metrics-server
if ($LASTEXITCODE -eq 0) {
    Write-Host "  [OK] Metrics Server enabled" -ForegroundColor Green
}

# Enable Dashboard
Write-Host "  Enabling dashboard..." -ForegroundColor Gray
minikube addons enable dashboard
if ($LASTEXITCODE -eq 0) {
    Write-Host "  [OK] Dashboard enabled" -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Setup Complete!" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Get Minikube IP
$minikubeIP = minikube ip
Write-Host "Minikube IP: $minikubeIP" -ForegroundColor Green
Write-Host ""

# Show kubectl context
Write-Host "Kubernetes Context:" -ForegroundColor Yellow
kubectl config current-context
Write-Host ""

# Deploy namespace and infrastructure
Write-Host "Deploying namespace and infrastructure..." -ForegroundColor Yellow
kubectl apply -f k8s/namespace.yaml
