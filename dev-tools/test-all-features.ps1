#Requires -Version 5.1
<#
.SYNOPSIS
    Non-regression test script for easy-input-filter.
    Builds the library, starts the playground app, runs all PASS/FAIL cases
    and prints a color-coded summary. Exit code 1 if any test fails.

.EXAMPLE
    # From repo root:
    .\dev-tools\test-all-features.ps1
#>

$ErrorActionPreference = "Stop"

# ---- Paths ------------------------------------------------------------------
$RepoRoot   = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$DevTools   = $PSScriptRoot
$JarPath    = Join-Path $DevTools "target\easy-input-filter-dev-tools-0.1.0-SNAPSHOT.jar"
$LogFile    = Join-Path $env:TEMP "playground-app.log"
$LogFileErr = Join-Path $env:TEMP "playground-app-err.log"
$BaseUrl    = "http://localhost:8081/playground"

# ---- Accumulated results ----------------------------------------------------
$script:Results = @()

# ---- Helpers ----------------------------------------------------------------
function Write-Header {
    param([string]$Msg)
    Write-Host ""
    Write-Host ("=" * 65) -ForegroundColor Cyan
    Write-Host "  $Msg" -ForegroundColor Cyan
    Write-Host ("=" * 65) -ForegroundColor Cyan
}

function Invoke-Http {
    param(
        [string]$Uri,
        [string]$Body   = $null,
        [string]$Method = "POST"
    )
    $params = @{
        Uri             = $Uri
        Method          = $Method
        UseBasicParsing = $true
        ErrorAction     = "Stop"
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body        = $Body
    }
    try {
        $resp = Invoke-WebRequest @params
        return [PSCustomObject]@{ Status = [int]$resp.StatusCode; Content = $resp.Content }
    } catch {
        $ex = $_.Exception
        if ($null -ne $ex.Response) {
            $status = [int]$ex.Response.StatusCode
            $content = ""
            try {
                $stream  = $ex.Response.GetResponseStream()
                $reader  = [System.IO.StreamReader]::new($stream)
                $content = $reader.ReadToEnd()
            } catch { }
            return [PSCustomObject]@{ Status = $status; Content = $content }
        }
        return [PSCustomObject]@{ Status = -1; Content = $ex.Message }
    }
}

function Add-TestResult {
    param(
        [string]$Name,
        [string]$Expected,
        [string]$Actual,
        [bool]  $Passed
    )
    $script:Results += [PSCustomObject]@{
        Name     = $Name
        Expected = $Expected
        Actual   = $Actual
        Passed   = $Passed
    }
}

function Test-HttpStatus {
    param([string]$Name, [string]$Uri, [string]$Body, [int]$ExpectedStatus)
    $resp   = Invoke-Http -Uri $Uri -Body $Body
    $passed = $resp.Status -eq $ExpectedStatus
    Add-TestResult -Name $Name `
        -Expected "HTTP $ExpectedStatus" `
        -Actual   "HTTP $($resp.Status)" `
        -Passed   $passed
}

function Test-HttpStatusAndValue {
    param(
        [string]$Name,
        [string]$Uri,
        [string]$Body,
        [int]   $ExpectedStatus,
        [string]$JsonKey,
        [string]$ExpectedValue
    )
    $resp     = Invoke-Http -Uri $Uri -Body $Body
    $statusOk = $resp.Status -eq $ExpectedStatus
    $actualVal = "(n/a)"
    $valueOk   = $true
    if ($statusOk -and $resp.Content) {
        try {
            $json      = $resp.Content | ConvertFrom-Json
            $actualVal = $json.$JsonKey
            $valueOk   = ($actualVal -eq $ExpectedValue)
        } catch {
            $valueOk = $false
        }
    }
    $passed  = $statusOk -and $valueOk
    $expDesc = "HTTP $ExpectedStatus + $JsonKey='$ExpectedValue'"
    $actDesc = "HTTP $($resp.Status) + $JsonKey='$actualVal'"
    Add-TestResult -Name $Name -Expected $expDesc -Actual $actDesc -Passed $passed
}

# ============================================================================
# STEP 1 — Build main reactor
# ============================================================================
Write-Header "STEP 1 -- Build main reactor (mvn clean install -DskipTests)"
Push-Location $RepoRoot
try {
    & mvn clean install -DskipTests -q
    if ($LASTEXITCODE -ne 0) { throw "Main reactor build failed (exit $LASTEXITCODE)" }
    Write-Host "  OK" -ForegroundColor Green
} finally {
    Pop-Location
}

# ============================================================================
# STEP 2 — Build playground
# ============================================================================
Write-Header "STEP 2 -- Build playground (mvn package -DskipTests)"
Push-Location $DevTools
try {
    & mvn clean package -DskipTests -q
    if ($LASTEXITCODE -ne 0) { throw "dev-tools build failed (exit $LASTEXITCODE)" }
    Write-Host "  OK" -ForegroundColor Green
} finally {
    Pop-Location
}

if (-not (Test-Path $JarPath)) {
    throw "Jar not found: $JarPath"
}

# ============================================================================
# STEP 3 — Start playground app
# ============================================================================
Write-Header "STEP 3 -- Start playground app (port 8081)"
"" | Out-File $LogFile    -Encoding utf8
"" | Out-File $LogFileErr -Encoding utf8
$proc = Start-Process `
    -FilePath    "java" `
    -ArgumentList @("-jar", $JarPath) `
    -RedirectStandardOutput $LogFile `
    -RedirectStandardError  $LogFileErr `
    -PassThru `
    -WindowStyle Hidden
Write-Host "  PID=$($proc.Id)  log: $LogFile"

# ============================================================================
# STEP 4 — Wait for readiness (poll up to 30 s)
# ============================================================================
Write-Header "STEP 4 -- Waiting for readiness (max 30 s)"
$ready   = $false
$timeout = 30

for ($i = 1; $i -le $timeout; $i++) {
    Start-Sleep -Seconds 1
    Write-Host "  [$i/$timeout] polling $BaseUrl/no-phone ..." -NoNewline
    try {
        $r = Invoke-Http -Uri "$BaseUrl/no-phone" -Body "{}"
        if ($r.Status -in @(200, 400)) {
            $ready = $true
            Write-Host " ready (HTTP $($r.Status))" -ForegroundColor Green
            break
        }
        Write-Host " HTTP $($r.Status)"
    } catch {
        Write-Host " not yet"
    }
}

if (-not $ready) {
    Write-Host "ERROR: app did not start within ${timeout}s." -ForegroundColor Red
    Write-Host "Check log: $LogFile" -ForegroundColor Yellow
    if (-not $proc.HasExited) { Stop-Process -Id $proc.Id -Force }
    exit 1
}

# ============================================================================
# STEP 5 — Run all tests
# ============================================================================
Write-Header "STEP 5 -- Running tests"

# @NoPhone
Test-HttpStatus "@NoPhone  FAIL (phone number -> 400)" `
    "$BaseUrl/no-phone" '{"value":"06 12 34 56 78"}' 400
Test-HttpStatus "@NoPhone  PASS (clean text -> 200)" `
    "$BaseUrl/no-phone" '{"value":"Bonjour monde"}' 200

# @NoEmail
Test-HttpStatus "@NoEmail  FAIL (email address -> 400)" `
    "$BaseUrl/no-email" '{"value":"test@example.com"}' 400
Test-HttpStatus "@NoEmail  PASS (clean text -> 200)" `
    "$BaseUrl/no-email" '{"value":"Bonjour monde"}' 200

# @NoUrl
Test-HttpStatus "@NoUrl    FAIL (https URL -> 400)" `
    "$BaseUrl/no-url" '{"value":"https://example.com/x"}' 400
Test-HttpStatus "@NoUrl    FAIL (wa.me bare domain -> 400)" `
    "$BaseUrl/no-url" '{"value":"wa.me/33612345678"}' 400
Test-HttpStatus "@NoUrl    PASS (clean text -> 200)" `
    "$BaseUrl/no-url" '{"value":"Bonjour monde"}' 200

# @NoKeywords
Test-HttpStatus "@NoKeywords FAIL (keyword 'whatsapp' -> 400)" `
    "$BaseUrl/no-keywords" '{"value":"Contactez-moi sur WhatsApp"}' 400
Test-HttpStatus "@NoKeywords PASS (clean text -> 200)" `
    "$BaseUrl/no-keywords" '{"value":"Contacter le vendeur"}' 200

# @NoHtml
Test-HttpStatus "@NoHtml   FAIL (script tag -> 400)" `
    "$BaseUrl/no-html" '{"value":"<script>alert(1)</script>"}' 400
Test-HttpStatus "@NoHtml   PASS (plain text -> 200)" `
    "$BaseUrl/no-html" '{"value":"Texte simple"}' 200

# @Sanitize  (SANITIZE strategy -- always 200, value may be modified)
Test-HttpStatusAndValue "@Sanitize  DIRTY (HTML+spaces sanitized -> 200)" `
    "$BaseUrl/sanitize" '{"value":"  <b>VIP</b>   client    fidele  "}' `
    200 "value" "VIP client fidele"
Test-HttpStatusAndValue "@Sanitize  CLEAN (unchanged -> 200)" `
    "$BaseUrl/sanitize" '{"value":"Texte propre"}' `
    200 "value" "Texte propre"

# @MaxRepeatChar(3)  (SANITIZE strategy -- always 200, runs collapsed)
Test-HttpStatusAndValue "@MaxRepeatChar(3) DIRTY (runs collapsed -> 200)" `
    "$BaseUrl/max-repeat-char" '{"value":"Suuuuuper!!!!!!"}' `
    200 "value" "Suuuper!!!"
Test-HttpStatusAndValue "@MaxRepeatChar(3) CLEAN (unchanged -> 200)" `
    "$BaseUrl/max-repeat-char" '{"value":"Super!"}' `
    200 "value" "Super!"

# @AllowedChars
Test-HttpStatus "@AllowedChars FAIL (letters not allowed -> 400)" `
    "$BaseUrl/allowed-chars" '{"value":"Hello World"}' 400
Test-HttpStatus "@AllowedChars PASS (digits/+/space -> 200)" `
    "$BaseUrl/allowed-chars" '{"value":"+33 6 12 34 56"}' 200

# @Honeypot
Test-HttpStatus "@Honeypot FAIL (trap field filled -> 400)" `
    "$BaseUrl/honeypot" '{"realField":"Bonjour","website":"http://spam.com"}' 400
Test-HttpStatus "@Honeypot PASS (trap field empty -> 200)" `
    "$BaseUrl/honeypot" '{"realField":"Bonjour"}' 200

# @RequestParam @NoPhone  [Vague A5 fix]
Test-HttpStatus "@RequestParam FAIL (phone in query param -> 400)  [A5]" `
    "$BaseUrl/request-param?phone=0612345678" $null 400
Test-HttpStatus "@RequestParam PASS (clean in query param -> 200)  [A5]" `
    "$BaseUrl/request-param?phone=Bonjour" $null 200

# Inherited field @NoPhone  [Vague A4 fix]
Test-HttpStatus "Inherited @NoPhone FAIL (parent field -> 400)  [A4]" `
    "$BaseUrl/inherited-field" '{"value":"06 12 34 56 78"}' 400
Test-HttpStatus "Inherited @NoPhone PASS (parent field clean -> 200) [A4]" `
    "$BaseUrl/inherited-field" '{"value":"Bonjour monde"}' 200

# Webhook trigger
Test-HttpStatus "@WebhookTrigger FAIL (phone -> 400, webhook if configured)" `
    "$BaseUrl/webhook-trigger" '{"value":"06 12 34 56 78"}' 400
Test-HttpStatus "@WebhookTrigger PASS (clean -> 200)" `
    "$BaseUrl/webhook-trigger" '{"value":"Bonjour monde"}' 200

# ============================================================================
# STEP 6 — Print summary
# ============================================================================
Write-Header "STEP 6 -- Summary"

$nameW = 52
$expW  = 38
$actW  = 38

$header = "{0,-$nameW} {1,-$expW} {2,-$actW} {3}" -f "Test", "Expected", "Actual", "Result"
Write-Host $header -ForegroundColor White
Write-Host ("-" * ($nameW + $expW + $actW + 10)) -ForegroundColor Gray

foreach ($r in $script:Results) {
    $color  = if ($r.Passed) { "Green" } else { "Red" }
    $symbol = if ($r.Passed) { "PASS" } else { "FAIL" }
    $line   = "{0,-$nameW} {1,-$expW} {2,-$actW} {3}" -f `
        $r.Name, $r.Expected, $r.Actual, $symbol
    Write-Host $line -ForegroundColor $color
}

Write-Host ("-" * ($nameW + $expW + $actW + 10)) -ForegroundColor Gray

$passed = ($script:Results | Where-Object { $_.Passed }).Count
$total  = $script:Results.Count
$failed = $total - $passed

Write-Host ""
if ($failed -eq 0) {
    Write-Host "  $passed / $total tests PASSED" -ForegroundColor Green
} else {
    Write-Host "  $passed / $total tests passed -- $failed FAILED" -ForegroundColor Red
}

# Highlight A4/A5 regressions specifically
$a4a5 = $script:Results | Where-Object { $_.Name -like "*[A4]*" -or $_.Name -like "*[A5]*" }
$regressions = $a4a5 | Where-Object { -not $_.Passed }
if ($regressions.Count -gt 0) {
    Write-Host ""
    Write-Host "  REGRESSION on A4/A5 fixes:" -ForegroundColor Red
    $regressions | ForEach-Object { Write-Host "    - $($_.Name)" -ForegroundColor Red }
}

# ============================================================================
# STEP 7 — Stop playground app
# ============================================================================
Write-Header "STEP 7 -- Stop playground app"
if (-not $proc.HasExited) {
    Stop-Process -Id $proc.Id -Force
    Write-Host "  Process PID=$($proc.Id) stopped." -ForegroundColor Green
} else {
    Write-Host "  Process already exited (code=$($proc.ExitCode))." -ForegroundColor Yellow
}

# ============================================================================
# Exit code
# ============================================================================
if ($failed -gt 0) { exit 1 }
exit 0
