[CmdletBinding()]
param(
    [ValidateRange(1024, 65535)][int]$Puerto = 8082,
    [switch]$Reiniciar,
    [switch]$SoloValidar,
    [string]$ArchivoConfiguracion
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (-not $ArchivoConfiguracion) { $ArchivoConfiguracion = Join-Path $projectRoot '.env.apisperu' }

function Read-DniSettings([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw 'No existe el archivo privado de configuración de DNI.'
    }
    if ((Get-Item -LiteralPath $Path).Length -gt 8192) {
        throw 'El archivo de configuración supera el tamaño permitido.'
    }
    $values = @{}
    foreach ($line in [IO.File]::ReadAllLines([IO.Path]::GetFullPath($Path))) {
        $entry = $line.Trim()
        if (-not $entry -or $entry.StartsWith('#')) { continue }
        $parts = $entry.Split('=', 2)
        if ($parts.Length -ne 2 -or $parts[0].Trim() -notin @('APISPERU_TOKEN', 'DNI_DAILY_LIMIT')) {
            throw 'El archivo admite solamente APISPERU_TOKEN y DNI_DAILY_LIMIT.'
        }
        $name = $parts[0].Trim()
        if ($values.ContainsKey($name)) { throw 'Hay una variable repetida en la configuración de DNI.' }
        $values[$name] = $parts[1].Trim()
    }
    $key = if ($values.ContainsKey('APISPERU_TOKEN')) { $values['APISPERU_TOKEN'] } else { '' }
    if ($key -and ($key.Length -gt 4096 -or $key -cnotmatch '^[A-Za-z0-9._~+/-]+=*$')) {
        throw 'La clave tiene un formato inválido. Pégala sin comillas ni la palabra Bearer.'
    }
    $limit = 100
    if ($values.ContainsKey('DNI_DAILY_LIMIT')) {
        if (-not [int]::TryParse($values['DNI_DAILY_LIMIT'], [ref]$limit) -or $limit -lt 1 -or $limit -gt 10000) {
            throw 'DNI_DAILY_LIMIT debe ser un número entre 1 y 10000.'
        }
    }
    return [pscustomobject]@{ Key = $key; Limit = $limit }
}

try { $settings = Read-DniSettings $ArchivoConfiguracion }
catch { Write-Error $_.Exception.Message; exit 1 }

if ($SoloValidar) {
    if ($settings.Key) { Write-Host 'Configuración válida. Clave presente; su valor no se muestra.' }
    else { Write-Host 'Archivo preparado. Falta pegar la nueva clave en APISPERU_TOKEN.' }
    exit 0
}
if (-not $settings.Key) {
    Write-Error 'Pega y guarda la nueva clave en el archivo privado .env.apisperu antes de iniciar.'
    exit 1
}

$maven = Get-Command mvn.cmd -ErrorAction Stop
if ($Reiniciar) {
    $portPattern = '(?<!\S)"?-Djetty\.http\.port=' + $Puerto + '"?(?=\s|$)'
    $processes = Get-CimInstance Win32_Process -Filter "name = 'java.exe'" | Where-Object {
        $_.CommandLine -and $_.CommandLine.Contains($projectRoot) -and
        $_.CommandLine -match $portPattern -and $_.CommandLine.Contains('jetty:run')
    }
    foreach ($process in $processes) { Stop-Process -Id $process.ProcessId -ErrorAction Stop }
}

$previousKey = [Environment]::GetEnvironmentVariable('APISPERU_TOKEN', 'Process')
$previousLimit = [Environment]::GetEnvironmentVariable('DNI_DAILY_LIMIT', 'Process')
Push-Location $projectRoot
try {
    $env:APISPERU_TOKEN = $settings.Key
    $env:DNI_DAILY_LIMIT = [string]$settings.Limit
    $repository = Join-Path $env:USERPROFILE '.m2/repository'
    Write-Host "Iniciando registro en http://127.0.0.1:$Puerto/login/paciente?action=register"
    & $maven.Source "-Dmaven.repo.local=$repository" "-Djetty.http.port=$Puerto" 'jetty:run'
    $result = $LASTEXITCODE
} finally {
    [Environment]::SetEnvironmentVariable('APISPERU_TOKEN', $previousKey, 'Process')
    [Environment]::SetEnvironmentVariable('DNI_DAILY_LIMIT', $previousLimit, 'Process')
    Pop-Location
}
exit $result
