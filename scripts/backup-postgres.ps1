[CmdletBinding()]
param(
    [Parameter()]
    [ValidateNotNullOrEmpty()]
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '..\backups'),

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

    & docker compose @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose falló con código $LASTEXITCODE."
    }
}

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$resolvedOutput = [System.IO.Path]::GetFullPath($OutputDirectory)
[System.IO.Directory]::CreateDirectory($resolvedOutput) | Out-Null

$timestamp = [DateTimeOffset]::UtcNow.ToString('yyyyMMddTHHmmssZ')
$nonce = [Guid]::NewGuid().ToString('N').Substring(0, 12)
$fileName = "pos-$timestamp-$nonce.dump"
$containerPath = "/tmp/$fileName"
$backupPath = Join-Path $resolvedOutput $fileName

Push-Location $projectRoot
try {
    Invoke-DockerCompose -Arguments @(
        'exec', '-T', $Service,
        'sh', '-eu', '-c',
        'pg_dump --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" --format=custom --no-owner --no-acl --file="$1"',
        '--', $containerPath
    )
    Invoke-DockerCompose -Arguments @('cp', "${Service}:${containerPath}", $backupPath)
} finally {
    & docker compose exec -T $Service rm -f $containerPath 2>$null
    Pop-Location
}

if (-not (Test-Path -LiteralPath $backupPath -PathType Leaf)) {
    throw 'El respaldo no fue copiado al equipo local.'
}

$hash = (Get-FileHash -LiteralPath $backupPath -Algorithm SHA256).Hash.ToLowerInvariant()
$checksumPath = "$backupPath.sha256"
Set-Content -LiteralPath $checksumPath -Encoding ascii -NoNewline -Value "$hash  $fileName"

[pscustomobject]@{
    Backup = $backupPath
    Checksum = $checksumPath
    Sha256 = $hash
    CreatedAtUtc = [DateTimeOffset]::UtcNow
}
