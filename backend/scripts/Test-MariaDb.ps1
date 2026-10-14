param(
    [string]$MariaDbHome,
    [string]$JavaHome
)
$ErrorActionPreference = 'Stop'
$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not $MariaDbHome) {
    $MariaDbHome = Get-ChildItem -LiteralPath (Join-Path $backendRoot '.tools\mariadb') -Directory | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $JavaHome) {
    $JavaHome = Get-ChildItem -LiteralPath (Join-Path $backendRoot '.tools\java21') -Directory | Select-Object -First 1 -ExpandProperty FullName
}
$installer = Join-Path $MariaDbHome 'bin\mariadb-install-db.exe'
$server = Join-Path $MariaDbHome 'bin\mariadbd.exe'
$client = Join-Path $MariaDbHome 'bin\mariadb.exe'
$admin = Join-Path $MariaDbHome 'bin\mariadb-admin.exe'
foreach ($binary in @($installer, $server, $client, $admin, (Join-Path $JavaHome 'bin\java.exe'))) {
    if (-not (Test-Path -LiteralPath $binary)) { throw "Required tool missing: $binary" }
}
$testData = Join-Path $backendRoot ('.tools\db-test-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testData | Out-Null
$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$listener.Start()
$port = $listener.LocalEndpoint.Port
$listener.Stop()
$testPassword = [Guid]::NewGuid().ToString('N')
$prior = @{}
foreach ($name in @('JAVA_HOME','PATH','MYSQL_PWD','TEST_DB_URL','TEST_DB_PASSWORD')) {
    $prior[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$process = $null
$testExitCode = 1
Push-Location $backendRoot
try {
    $initialization = & $installer "--datadir=$testData" "--password=$testPassword" "--port=$port" 2>&1
    if ($LASTEXITCODE -ne 0) { throw 'MariaDB test initialization failed.' }
    $process = Start-Process -FilePath $server -ArgumentList @(
        '--no-defaults', ('--basedir="' + $MariaDbHome + '"'), ('--datadir="' + $testData + '"'),
        '--bind-address=127.0.0.1', "--port=$port", '--character-set-server=utf8mb4',
        '--collation-server=utf8mb4_unicode_ci'
    ) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $testData 'stdout.log') -RedirectStandardError (Join-Path $testData 'stderr.log')
    $env:MYSQL_PWD = $testPassword
    $ready = $false
    for ($attempt = 0; $attempt -lt 40; $attempt++) {
        if ($process.HasExited) { throw 'Isolated MariaDB test process stopped unexpectedly. Inspect its local stderr.log.' }
        & $admin --protocol=tcp --host=127.0.0.1 "--port=$port" --user=root ping --silent 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Milliseconds 250
    }
    if (-not $ready) { throw 'Isolated MariaDB test process did not become ready.' }
    & $client --protocol=tcp --host=127.0.0.1 "--port=$port" --user=root -e 'CREATE DATABASE health_habit_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
    if ($LASTEXITCODE -ne 0) { throw 'Test database creation failed.' }
    $env:TEST_DB_URL = "jdbc:mariadb://127.0.0.1:$port/health_habit_test"
    $env:TEST_DB_PASSWORD = $testPassword
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome\bin;" + $prior['PATH']
    & (Join-Path $backendRoot 'mvnw.cmd') test '-Dspring.profiles.active=mariadb-test'
    $testExitCode = $LASTEXITCODE
    if ($testExitCode -eq 0) {
        & $client --protocol=tcp --host=127.0.0.1 "--port=$port" --user=root health_habit_test -e 'SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank'
    }
} finally {
    if ($process -and -not $process.HasExited) {
        & $admin --protocol=tcp --host=127.0.0.1 "--port=$port" --user=root shutdown 2>$null | Out-Null
        if (-not $process.WaitForExit(10000)) { Stop-Process -Id $process.Id }
    }
    foreach ($name in $prior.Keys) { [Environment]::SetEnvironmentVariable($name, $prior[$name], 'Process') }
    Pop-Location
    Write-Host "Isolated test data retained at: $testData (server stopped)."
}
exit $testExitCode
