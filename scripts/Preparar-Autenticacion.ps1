[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$ruta = Join-Path (Split-Path $PSScriptRoot -Parent) '.env.auth'
if (Test-Path -LiteralPath $ruta) {
    Write-Host 'La configuración privada de autenticación ya existe; se conserva la clave.'
    return
}
$bytes = New-Object byte[] 32
$generador = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $generador.GetBytes($bytes) } finally { $generador.Dispose() }
$contenido = 'AUTH_JWT_KEY=' + [Convert]::ToBase64String($bytes) + [Environment]::NewLine
[IO.File]::WriteAllText($ruta, $contenido, (New-Object Text.UTF8Encoding($false)))
Write-Host 'Configuración privada creada en .env.auth. La clave no se muestra ni se incluye en Git.'
