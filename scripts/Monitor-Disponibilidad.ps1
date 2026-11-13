param(
    [string]$Url = 'https://clinicainternacional-production.up.railway.app/health/ready',
    [ValidateRange(1, 180)][int]$Minutos = 60,
    [ValidateRange(15, 240)][int]$IntervaloSegundos = 180
)

$ErrorActionPreference = 'Stop'
$destino = [Uri]$Url
$local = $destino.Scheme -in @('http', 'https') -and $destino.Host -in @('localhost', '127.0.0.1')
$railway = $destino.Scheme -eq 'https' -and $destino.Host -eq 'clinicainternacional-production.up.railway.app' -and $destino.IsDefaultPort
if ((-not $local -and -not $railway) -or $destino.AbsolutePath -ne '/health/ready' -or $destino.Query -or $destino.UserInfo) {
    throw 'Usa únicamente /health/ready de la clínica en Railway o localhost, sin credenciales ni parámetros.'
}
$carpeta = Join-Path $PSScriptRoot '../target/disponibilidad'
New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
$salida = Join-Path $carpeta ('monitor-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.csv')
$limite = [DateTimeOffset]::UtcNow.AddMinutes($Minutos)
Write-Host "Monitor limitado a $Minutos minutos. Ctrl+C para detener. Evidencia: $salida"

while ([DateTimeOffset]::UtcNow -lt $limite) {
    $reloj = [Diagnostics.Stopwatch]::StartNew()
    $estado = 0
    $disponible = $false
    try {
        $respuesta = Invoke-WebRequest -Uri $destino -Method Get -TimeoutSec 20 -MaximumRedirection 0 -UseBasicParsing
        $estado = [int]$respuesta.StatusCode
        $cuerpo = $respuesta.Content | ConvertFrom-Json
        $disponible = $estado -eq 200 -and $cuerpo.code -eq 'SISTEMA_DISPONIBLE'
    } catch {
        if ($null -ne $_.Exception.Response) { $estado = [int]$_.Exception.Response.StatusCode }
    }
    $reloj.Stop()
    $fila = [pscustomobject]@{
        fechaUTC = [DateTimeOffset]::UtcNow.ToString('o')
        estadoHTTP = $estado
        disponible = $disponible
        milisegundos = $reloj.ElapsedMilliseconds
    }
    $fila | Export-Csv -LiteralPath $salida -NoTypeInformation -Append -Encoding utf8
    Write-Host "$($fila.fechaUTC) HTTP=$estado disponible=$disponible tiempo=$($fila.milisegundos)ms"
    $restante = ($limite - [DateTimeOffset]::UtcNow).TotalSeconds
    $pausa = [Math]::Min($restante, $IntervaloSegundos)
    if ($pausa -gt 0) { Start-Sleep -Milliseconds ([int]($pausa * 1000)) }
}
