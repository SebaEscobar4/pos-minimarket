[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateScript({ Test-Path -LiteralPath $_ -PathType Leaf })]
    [string]$BackupPath,

    [Parameter()]
    [ValidatePattern('^[a-zA-Z0-9][a-zA-Z0-9_-]{0,62}$')]
    [string]$Service = 'postgres'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

function Invoke-DockerCompose {
    param(
        [Parameter(Mandatory)]
        [string[]]$Arguments
    )

    $output = & docker compose @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose falló con código $LASTEXITCODE."
    }
    return $output
}

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$resolvedBackup = [System.IO.Path]::GetFullPath($BackupPath)
$checksumPath = "$resolvedBackup.sha256"
if (-not (Test-Path -LiteralPath $checksumPath -PathType Leaf)) {
    throw "Falta el checksum requerido: $checksumPath"
}

$checksumLine = (Get-Content -LiteralPath $checksumPath -Raw).Trim()
if ($checksumLine -notmatch '^([0-9a-fA-F]{64})\s{2}(.+)$') {
    throw 'El archivo SHA-256 no tiene el formato esperado.'
}
$expectedHash = $Matches[1].ToLowerInvariant()
$expectedFileName = $Matches[2]
if ($expectedFileName -ne [System.IO.Path]::GetFileName($resolvedBackup)) {
    throw 'El checksum corresponde a un archivo de respaldo diferente.'
}
$actualHash = (Get-FileHash -LiteralPath $resolvedBackup -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actualHash -ne $expectedHash) {
    throw 'El respaldo no coincide con su checksum SHA-256.'
}

$nonce = [Guid]::NewGuid().ToString('N').Substring(0, 12)
$restoreDatabase = "pos_restore_$nonce"
$containerPath = "/tmp/pos-restore-$nonce.dump"
$databaseCreated = $false
$startedAt = [DateTimeOffset]::UtcNow

$reconciliationSql = @'
SELECT jsonb_build_object(
    'migrations', (SELECT COUNT(*) FROM flyway_schema_history WHERE success),
    'metadata', (SELECT COUNT(*) FROM app_metadata),
    'users', (SELECT COUNT(*) FROM app_user),
    'authenticationEvents', (SELECT COUNT(*) FROM authentication_audit_event),
    'products', (SELECT COUNT(*) FROM catalog_product),
    'inventoryMovements', (SELECT COUNT(*) FROM inventory_movement),
    'inventoryQuantity', (SELECT COALESCE(SUM(quantity), 0) FROM inventory_balance),
    'cashSessions', (SELECT COUNT(*) FROM cash_session),
    'cashMovements', (SELECT COUNT(*) FROM cash_movement),
    'sales', (SELECT COUNT(*) FROM sale),
    'saleLines', (SELECT COUNT(*) FROM sale_line),
    'salesTotal', (SELECT COALESCE(SUM(total), 0) FROM sale),
    'payments', (SELECT COUNT(*) FROM payment),
    'cancellations', (SELECT COUNT(*) FROM sale_cancellation)
)::text;
'@

Push-Location $projectRoot
try {
    Invoke-DockerCompose -Arguments @('cp', $resolvedBackup, "${Service}:${containerPath}") | Out-Null
    Invoke-DockerCompose -Arguments @(
        'exec', '-T', $Service,
        'sh', '-eu', '-c',
        'createdb --username="$POSTGRES_USER" "$1"',
        '--', $restoreDatabase
    ) | Out-Null
    $databaseCreated = $true

    Invoke-DockerCompose -Arguments @(
        'exec', '-T', $Service,
        'sh', '-eu', '-c',
        'pg_restore --username="$POSTGRES_USER" --dbname="$1" --no-owner --no-acl --exit-on-error "$2"',
        '--', $restoreDatabase, $containerPath
    ) | Out-Null

    $sourceSnapshot = (Invoke-DockerCompose -Arguments @(
        'exec', '-T', $Service,
        'sh', '-eu', '-c',
        'psql --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --tuples-only --no-align --command="$1"',
        '--', $reconciliationSql
    ) | Out-String).Trim()
    $restoredSnapshot = (Invoke-DockerCompose -Arguments @(
        'exec', '-T', $Service,
        'sh', '-eu', '-c',
        'psql --username="$POSTGRES_USER" --dbname="$1" --tuples-only --no-align --command="$2"',
        '--', $restoreDatabase, $reconciliationSql
    ) | Out-String).Trim()

    if ($sourceSnapshot -ne $restoredSnapshot) {
        throw "La reconciliación falló.`nOrigen: $sourceSnapshot`nRestaurado: $restoredSnapshot"
    }

    [pscustomobject]@{
        Backup = $resolvedBackup
        IsolatedDatabase = $restoreDatabase
        StartedAtUtc = $startedAt
        DurationSeconds = [math]::Round(([DateTimeOffset]::UtcNow - $startedAt).TotalSeconds, 2)
        Reconciliation = $restoredSnapshot
        Result = 'OK'
    }
} finally {
    if ($databaseCreated) {
        & docker compose exec -T $Service sh -eu -c 'dropdb --username="$POSTGRES_USER" --if-exists --force "$1"' -- $restoreDatabase 2>$null
    }
    & docker compose exec -T $Service rm -f $containerPath 2>$null
    Pop-Location
}
