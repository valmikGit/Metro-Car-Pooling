$ErrorActionPreference = "Stop"

Write-Host "Starting restart of all services..."

# 1. Bring down all services
Write-Host "Stopping all services..."

# Find all docker-compose.yml files, excluding node_modules
$services = Get-ChildItem -Recurse -Filter "docker-compose.yml" | Where-Object { $_.FullName -notmatch "node_modules" }

foreach ($composeFile in $services) {
    $serviceDir = $composeFile.DirectoryName
    Write-Host "Stopping service in $serviceDir"
    Push-Location $serviceDir
    try {
        docker compose down -v
    } finally {
        Pop-Location
    }
}

Write-Host "All services stopped."

# 2. Start infra service first (priority)
if (Test-Path "infra") {
    Write-Host "Starting infra service..."
    Push-Location "infra"
    try {
        # Removed --build as requested
        docker compose up -d
    } finally {
        Pop-Location
    }
    
    Write-Host "Infra service started. Waiting for 15 seconds to allow initialization..."
    Start-Sleep -Seconds 15
} else {
    Write-Error "infra directory not found!"
    exit 1
}

# 3. Start all other services
Write-Host "Starting other services..."

foreach ($composeFile in $services) {
    $serviceDir = $composeFile.DirectoryName
    
    # Skip infra as it's already started
    # Check if the directory name is 'infra' or ends with '\infra'
    if ($serviceDir -match "\\infra$|/infra$") {
        continue
    }
    
    Write-Host "Starting service in $serviceDir"
    Push-Location $serviceDir
    try {
        # Removed --build as requested
        docker compose up -d
    } finally {
        Pop-Location
    }
}

Write-Host "All services restart sequence initiated."
docker ps
