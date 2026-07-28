#!/usr/bin/env powershell

# ServiceNow File Uploader - PowerShell Runner

param(
    [Parameter(Position=0)]
    [string]$Mode = "interactive"
)

$scriptDir = Split-Path -Parent -Path $MyInvocation.MyCommand.Definition
$className = "ServiceNowFileUploader"
$sourceFile = Join-Path $scriptDir "$className.java"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   ServiceNow File Uploader" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Java is installed
try {
    $javaVersion = java -version 2>&1
    Write-Host "[OK] Java found" -ForegroundColor Green
} catch {
    Write-Host "[ERROR] Java is not installed or not in PATH" -ForegroundColor Red
    exit 1
}

# Check if source file exists
if (-not (Test-Path $sourceFile)) {
    Write-Host "[ERROR] Source file not found: $sourceFile" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Source file found" -ForegroundColor Green
Write-Host ""

# Compile
Write-Host "[INFO] Compiling $className.java..." -ForegroundColor Yellow
javac "$sourceFile" 2>&1 | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compilation failed" -ForegroundColor Red
    exit 1
}

Write-Host "[OK] Compilation successful" -ForegroundColor Green
Write-Host ""

# Handle different modes
if ($Mode -eq "--help") {
    Write-Host "Usage: .\run.ps1 [mode]" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Modes:" -ForegroundColor Cyan
    Write-Host "  (default)  Interactive mode - you will be prompted for all values"
    Write-Host "  --config   Load values from config.properties file"
    Write-Host "  --help     Show this help message"
    Write-Host ""
    exit 0
}

if ($Mode -eq "--config") {
    Write-Host "[INFO] Loading configuration from config.properties..." -ForegroundColor Yellow
    
    $configFile = Join-Path $scriptDir "config.properties"
    
    if (-not (Test-Path $configFile)) {
        Write-Host "[ERROR] config.properties not found at: $configFile" -ForegroundColor Red
        Write-Host "Please create config.properties file first" -ForegroundColor Red
        exit 1
    }

    # Read config file
    $config = @{}
    $fileContent = Get-Content $configFile -Raw
    
    $lines = $fileContent -split "`n"
    foreach ($line in $lines) {
        $line = $line.Trim()
        if ($line -and -not $line.StartsWith("#") -and -not $line.StartsWith("REM")) {
            $equalPos = $line.IndexOf('=')
            if ($equalPos -gt 0) {
                $key = $line.Substring(0, $equalPos).Trim()
                $value = $line.Substring($equalPos + 1).Trim()
                $config[$key] = $value
            }
        }
    }

    Write-Host "[OK] Configuration loaded" -ForegroundColor Green
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host "Running ServiceNow File Uploader" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host ""

    # Check for required config values
    $requiredKeys = @("FILE_PATH", "ENDPOINT", "QUERY_STRING", "CLIENT_ID", "CLIENT_SECRET", "USERNAME", "PASSWORD", "DELETE_FILE_FLAG")
    $missingKeys = @()

    foreach ($key in $requiredKeys) {
        if (-not $config.ContainsKey($key) -or [string]::IsNullOrEmpty($config[$key])) {
            $missingKeys += $key
        }
    }

    if ($missingKeys.Count -gt 0) {
        Write-Host "[ERROR] Missing or empty required config values:" -ForegroundColor Red
        foreach ($key in $missingKeys) {
            Write-Host "  - $key" -ForegroundColor Red
        }
        Write-Host "Please check your config.properties file and try again." -ForegroundColor Red
        exit 1
    }

    # Create a temporary batch file to run Java
    $tempBatFile = Join-Path $env:USERPROFILE "run_servicenow_$([System.Guid]::NewGuid()).bat"

    $batContent = @"
@echo off
java -cp "$scriptDir" $className "$($config['FILE_PATH'])" "$($config['ENDPOINT'])" "$($config['QUERY_STRING'])" "$($config['CLIENT_ID'])" "$($config['CLIENT_SECRET'])" "$($config['REFRESH_TOKEN'])" "$($config['REFRESH_TOKEN_FLAG'])" "$($config['USERNAME'])" "$($config['PASSWORD'])" "$($config['DELETE_FILE_FLAG'])"
"@

    $batContent | Out-File -FilePath $tempBatFile -Encoding ASCII

    Write-Host "[INFO] Running Java via temporary batch file:" -ForegroundColor Yellow
    Write-Host "  $tempBatFile"
    Write-Host ""

    # Run the batch file
    & $tempBatFile

    $exitCode = $LASTEXITCODE

    # Clean up the temporary batch file
    Remove-Item -Path $tempBatFile -Force

    Write-Host ""
    Write-Host "Java process exited with code: $exitCode" -ForegroundColor Yellow
    
    exit $exitCode
}

# Default: Interactive mode
Write-Host "[INFO] Running in interactive mode" -ForegroundColor Yellow
Write-Host "Please answer the following prompts:" -ForegroundColor Yellow
Write-Host ""

java -cp "$scriptDir" $className
exit $LASTEXITCODE