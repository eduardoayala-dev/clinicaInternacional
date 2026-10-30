[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$archivo=Join-Path $PSScriptRoot '../.env.mail'
if(-not (Test-Path -LiteralPath $archivo -PathType Leaf)){throw 'Falta .env.mail. Autoriza primero el buzón Gmail.'}
$lineas=[IO.File]::ReadAllLines($archivo)
if($lineas | Where-Object {$_ -match '^ACCOUNT_VERIFICATION_KEY=.+$'}){
    Write-Host 'La clave de verificación ya está configurada. No se reemplazó.'
    return
}
$bytes=New-Object byte[] 32
$generador=[Security.Cryptography.RandomNumberGenerator]::Create()
try{$generador.GetBytes($bytes)}finally{$generador.Dispose()}
$lineas=@($lineas | Where-Object {$_ -notmatch '^ACCOUNT_VERIFICATION_KEY=' -and $_ -notmatch '^APP_PUBLIC_URL='})
$lineas+='ACCOUNT_VERIFICATION_KEY='+[Convert]::ToBase64String($bytes)
[IO.File]::WriteAllLines($archivo,$lineas,(New-Object Text.UTF8Encoding($false)))
Write-Host 'Clave privada de códigos preparada en .env.mail, sin mostrarla.'
