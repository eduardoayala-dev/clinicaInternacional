[CmdletBinding()]
param([switch]$Jwt,[switch]$Correo,[switch]$Gemini)

$ErrorActionPreference = 'Stop'
$proyecto = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
foreach ($archivo in @('.env', '.env.apisperu')) {
    if (-not (Test-Path -LiteralPath (Join-Path $proyecto $archivo) -PathType Leaf)) {
        throw "Falta el archivo privado $archivo en la raíz del proyecto."
    }
}

Push-Location $proyecto
try {
    $opciones = @('compose', '--project-name', 'clinicainternacional', '--env-file', '.env', '--env-file', '.env.apisperu')
    if ($Jwt) {
        if (-not (Test-Path -LiteralPath '.env.auth' -PathType Leaf)) { throw 'Falta .env.auth con AUTH_JWT_KEY.' }
        $opciones += @('--env-file', '.env.auth')
    }
    if ($Correo) {
        if (-not $Jwt) { throw 'La recuperación requiere iniciar con -Jwt -Correo.' }
        if (-not (Test-Path -LiteralPath '.env.mail' -PathType Leaf)) { throw 'Falta .env.mail.' }
        $opciones += @('--env-file', '.env.mail')
    }
    if ($Gemini) {
        if (-not (Test-Path -LiteralPath '.env.gemini' -PathType Leaf)) { throw 'Falta .env.gemini; usa config/gemini.env.example como plantilla.' }
        $opciones += @('--env-file', '.env.gemini')
    }
    $opciones += @('-f', 'docker-compose.yml', '-f', 'compose.dni.yml')
    if ($Jwt) { $opciones += @('-f', 'compose.auth.yml') }
    if ($Correo) { $opciones += @('-f', 'compose.mail.yml') }
    if ($Gemini) { $opciones += @('-f', 'compose.gemini.yml') }
    & docker @opciones up -d --build app
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar el entorno local. Revisa Docker y la configuración privada.' }
    Write-Host 'Entorno local completo: http://localhost:8080/login/paciente'
    Write-Host 'Personal: http://localhost:8080/login/personal'
    Write-Host 'La consulta de DNI utiliza la configuración privada existente. La clave no se muestra.'
} finally {
    Pop-Location
}
