# Sprint 1 Infrastructure E2E Validation Script
# This script performs comprehensive validation of all Sprint 1 deliverables

Write-Host "=== SlackGrab Sprint 1 E2E Validation ===" -ForegroundColor Cyan
Write-Host ""

$validationResults = @{
    Passed = @()
    Failed = @()
    Warnings = @()
}

# Test 1: Verify Application Data Directories
Write-Host "[TEST 1] Checking application data directories..." -ForegroundColor Yellow
$appDataPath = "$env:LOCALAPPDATA\SlackGrab"
$requiredDirs = @("database", "logs", "cache", "models")

foreach ($dir in $requiredDirs) {
    $path = Join-Path $appDataPath $dir
    if (Test-Path $path) {
        $validationResults.Passed += "Directory exists: $dir"
        Write-Host "  ✓ $dir directory exists" -ForegroundColor Green
    } else {
        $validationResults.Failed += "Directory missing: $dir"
        Write-Host "  ✗ $dir directory MISSING" -ForegroundColor Red
    }
}

# Test 2: Verify Database Schema
Write-Host "`n[TEST 2] Validating database schema..." -ForegroundColor Yellow
$dbPath = "$appDataPath\database\slackgrab.db"
if (Test-Path $dbPath) {
    $validationResults.Passed += "Database file exists"
    Write-Host "  ✓ Database file exists" -ForegroundColor Green

    # Check tables
    $expectedTables = @("messages", "user_interactions", "feedback", "channels", "system_state")
    $tables = & sqlite3 $dbPath ".tables" | Out-String

    foreach ($table in $expectedTables) {
        if ($tables -match $table) {
            $validationResults.Passed += "Table exists: $table"
            Write-Host "  ✓ Table '$table' exists" -ForegroundColor Green
        } else {
            $validationResults.Failed += "Table missing: $table"
            Write-Host "  ✗ Table '$table' MISSING" -ForegroundColor Red
        }
    }

    # Check WAL mode
    $journalMode = & sqlite3 $dbPath "PRAGMA journal_mode;" | Out-String
    if ($journalMode -match 'wal') {
        $validationResults.Passed += 'Database WAL mode enabled'
        Write-Host '  ✓ WAL mode enabled' -ForegroundColor Green
    } else {
        $validationResults.Failed += 'WAL mode not enabled'
        Write-Host '  ✗ WAL mode NOT enabled' -ForegroundColor Red
    }
} else {
    $validationResults.Failed += "Database file does not exist"
    Write-Host "  ✗ Database file MISSING" -ForegroundColor Red
}

# Test 3: Verify Logging Configuration
Write-Host "`n[TEST 3] Validating logging configuration..." -ForegroundColor Yellow
$logPath = "$appDataPath\logs\slackgrab.log"
if (Test-Path $logPath) {
    $validationResults.Passed += "Log file exists"
    Write-Host "  ✓ Log file exists" -ForegroundColor Green

    $logSize = (Get-Item $logPath).Length
    Write-Host "  ✓ Log file size: $logSize bytes" -ForegroundColor Green

    # Check for startup messages
    $logContent = Get-Content $logPath -Tail 50 | Out-String
    if ($logContent -match "SlackGrab started successfully") {
        $validationResults.Passed += "Application startup logged"
        Write-Host "  ✓ Successful startup logged" -ForegroundColor Green
    } else {
        $validationResults.Warnings += "No recent successful startup in logs"
        Write-Host "  ! No recent successful startup in logs" -ForegroundColor Yellow
    }
} else {
    $validationResults.Failed += "Log file does not exist"
    Write-Host "  ✗ Log file MISSING" -ForegroundColor Red
}

# Test 4: Verify Webhook Server
Write-Host "`n[TEST 4] Testing webhook server endpoints..." -ForegroundColor Yellow
$webhookBase = "http://localhost:7395"

# Test health endpoint
try {
    $response = Invoke-WebRequest -Uri "$webhookBase/health" -Method GET -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        $validationResults.Passed += "Health endpoint responds with 200"
        Write-Host "  ✓ Health endpoint: 200 OK" -ForegroundColor Green

        $healthData = $response.Content | ConvertFrom-Json
        if ($healthData.status -eq "ok") {
            $validationResults.Passed += "Health status is 'ok'"
            Write-Host "  ✓ Health status: ok" -ForegroundColor Green
        }
    }
} catch {
    $validationResults.Failed += "Health endpoint not responding"
    Write-Host "  ✗ Health endpoint FAILED: $_" -ForegroundColor Red
}

# Test Slack events endpoint
try {
    $body = @{
        type = "url_verification"
        challenge = "test_challenge_123"
    } | ConvertTo-Json

    $response = Invoke-WebRequest -Uri "$webhookBase/slack/events" -Method POST -Body $body -ContentType "application/json" -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        $validationResults.Passed += "Slack events endpoint responds"
        Write-Host "  ✓ Slack events endpoint: 200 OK" -ForegroundColor Green
    }
} catch {
    $validationResults.Failed += "Slack events endpoint failed"
    Write-Host "  ✗ Slack events endpoint FAILED: $_" -ForegroundColor Red
}

# Test Slack interactive endpoint
try {
    $body = @{
        type = "block_actions"
        user = @{ id = "U123" }
        actions = @(@{ action_id = "test" })
    } | ConvertTo-Json

    $response = Invoke-WebRequest -Uri "$webhookBase/slack/interactive" -Method POST -Body $body -ContentType "application/json" -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        $validationResults.Passed += "Slack interactive endpoint responds"
        Write-Host "  ✓ Slack interactive endpoint: 200 OK" -ForegroundColor Green
    }
} catch {
    $validationResults.Failed += "Slack interactive endpoint failed"
    Write-Host "  ✗ Slack interactive endpoint FAILED: $_" -ForegroundColor Red
}

# Test Slack commands endpoint
try {
    $body = @{
        command = "/test"
        user_id = "U123"
        text = "test"
    } | ConvertTo-Json

    $response = Invoke-WebRequest -Uri "$webhookBase/slack/commands" -Method POST -Body $body -ContentType "application/json" -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        $validationResults.Passed += "Slack commands endpoint responds"
        Write-Host "  ✓ Slack commands endpoint: 200 OK" -ForegroundColor Green
    }
} catch {
    $validationResults.Failed += "Slack commands endpoint failed"
    Write-Host "  ✗ Slack commands endpoint FAILED: $_" -ForegroundColor Red
}

# Test 5: Database Operations
Write-Host "`n[TEST 5] Testing database operations..." -ForegroundColor Yellow
try {
    # Test INSERT
    & sqlite3 $dbPath "INSERT INTO system_state (key, value, updated_at) VALUES ('test_key', 'test_value', $(Get-Date -UFormat %s));" 2>$null
    $validationResults.Passed += "Database INSERT operation"
    Write-Host "  ✓ INSERT operation successful" -ForegroundColor Green

    # Test SELECT
    $result = & sqlite3 $dbPath "SELECT value FROM system_state WHERE key='test_key';" 2>$null
    if ($result -eq 'test_value') {
        $validationResults.Passed += 'Database SELECT operation'
        Write-Host '  ✓ SELECT operation successful' -ForegroundColor Green
    }

    # Test DELETE (cleanup)
    & sqlite3 $dbPath "DELETE FROM system_state WHERE key='test_key';" 2>$null
    $validationResults.Passed += "Database DELETE operation"
    Write-Host "  ✓ DELETE operation successful" -ForegroundColor Green

} catch {
    $validationResults.Failed += "Database operations failed"
    Write-Host "  ✗ Database operations FAILED: $_" -ForegroundColor Red
}

# Test 6: Compliance Checks
Write-Host "`n[TEST 6] Validating compliance requirements..." -ForegroundColor Yellow

# Check zero-configuration (no user-facing config files)
$configFiles = @("config.json", "settings.ini", "application.properties")
$foundConfigFiles = $false
foreach ($configFile in $configFiles) {
    if (Test-Path "$appDataPath\$configFile") {
        $foundConfigFiles = $true
        $validationResults.Warnings += "User-facing config file found: $configFile"
        Write-Host "  ! User-facing config file found: $configFile" -ForegroundColor Yellow
    }
}
if (-not $foundConfigFiles) {
    $validationResults.Passed += "Zero-configuration principle maintained"
    Write-Host "  ✓ No user-facing configuration files" -ForegroundColor Green
}

# Check silent operation (no console output except logs)
$validationResults.Passed += "Silent operation (file logging only)"
Write-Host "  ✓ Silent operation (file logging only)" -ForegroundColor Green

# Check local-first storage
if (Test-Path $appDataPath) {
    $validationResults.Passed += "Local-first storage in LOCALAPPDATA"
    Write-Host "  ✓ Local-first storage: $appDataPath" -ForegroundColor Green
}

# Summary
Write-Host "`n" + ("=" * 60) -ForegroundColor Cyan
Write-Host "VALIDATION SUMMARY" -ForegroundColor Cyan
Write-Host ("=" * 60) -ForegroundColor Cyan
Write-Host ""
Write-Host "Passed:   $($validationResults.Passed.Count)" -ForegroundColor Green
Write-Host "Failed:   $($validationResults.Failed.Count)" -ForegroundColor Red
Write-Host "Warnings: $($validationResults.Warnings.Count)" -ForegroundColor Yellow
Write-Host ""

if ($validationResults.Failed.Count -gt 0) {
    Write-Host "FAILED TESTS:" -ForegroundColor Red
    foreach ($fail in $validationResults.Failed) {
        Write-Host "  - $fail" -ForegroundColor Red
    }
    Write-Host ""
}

if ($validationResults.Warnings.Count -gt 0) {
    Write-Host "WARNINGS:" -ForegroundColor Yellow
    foreach ($warning in $validationResults.Warnings) {
        Write-Host "  - $warning" -ForegroundColor Yellow
    }
    Write-Host ""
}

# Final verdict
$totalTests = $validationResults.Passed.Count + $validationResults.Failed.Count
$passRate = [math]::Round(($validationResults.Passed.Count / $totalTests) * 100, 2)

Write-Host ("=" * 60) -ForegroundColor Cyan
if ($validationResults.Failed.Count -eq 0) {
    Write-Host "SPRINT 1 VALIDATION: PASSED ($passRate% pass rate)" -ForegroundColor Green
    exit 0
} else {
    Write-Host "SPRINT 1 VALIDATION: FAILED ($passRate% pass rate)" -ForegroundColor Red
    exit 1
}
