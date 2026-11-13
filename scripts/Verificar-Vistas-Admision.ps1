[CmdletBinding()]
param(
    [string]$RepositorioMaven = (Join-Path $env:USERPROFILE '.m2/repository'),
    [string]$VersionTomcat = '10.1.61',
    [ValidateSet(11,17)][int]$NivelJava = 11,
    [string[]]$VistasAdicionales = @()
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$proyecto = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$salida = Join-Path $proyecto 'target/verificacion-vistas-admision'
$bibliotecas = Join-Path $proyecto 'target/clinicaInternacional/WEB-INF/lib'
if (-not (Test-Path -LiteralPath $bibliotecas)) { throw 'Ejecuta mvn package antes de comprobar las vistas.' }

$tomcat = @(Get-ChildItem (Join-Path $RepositorioMaven 'org/apache/tomcat') -Recurse -Filter '*.jar' | Where-Object { $_.Directory.Name -eq $VersionTomcat -and $_.FullName -notmatch 'tomcat-embed' })
if (-not ($tomcat.Name -contains "tomcat-jasper-$VersionTomcat.jar")) { throw "No se encontró Jasper $VersionTomcat en el repositorio Maven indicado." }
$ant = @(Get-ChildItem (Join-Path $RepositorioMaven 'org/apache/ant') -Recurse -Filter '*.jar')
$ecj = Get-ChildItem (Join-Path $RepositorioMaven 'org/eclipse/jdt/ecj') -Recurse -Filter '*.jar' | Sort-Object FullName | Select-Object -Last 1
if ($ant.Count -eq 0 -or $null -eq $ecj) { throw 'La comprobación requiere Ant y el compilador ECJ en el repositorio Maven.' }
$classpath = (@($tomcat.FullName) + @($ant.FullName) + @($ecj.FullName) + @(Get-ChildItem $bibliotecas -Filter '*.jar' | ForEach-Object FullName) + @((Join-Path $proyecto 'target/classes'))) -join [IO.Path]::PathSeparator
$vistas = @('dashboard','medicos','editar-medico','pacientes','horarios','seguimiento-confirmaciones','seguimiento-ia','reasignaciones','dashboard_operativo') | ForEach-Object { "WEB-INF/views/admin/$_.jsp" }
New-Item -ItemType Directory -Force -Path $salida | Out-Null
$vistas += @('WEB-INF/views/catalogo/motivos.jsp','WEB-INF/views/catalogo/error.jsp','WEB-INF/views/catalogo/importar-motivos.jsp')
$vistas += $VistasAdicionales
$registro = Join-Path $salida 'compilacion.log'
& java -cp $classpath org.apache.jasper.JspC -uriroot (Join-Path $proyecto 'src/main/webapp') -d $salida -compile -source $NivelJava -target $NivelJava @vistas *> $registro
$codigo = $LASTEXITCODE
$texto = Get-Content -Raw -LiteralPath $registro
if ($codigo -ne 0 -or $texto -notmatch 'completed with \[0\] errors') {
    Get-Content -LiteralPath $registro | Write-Output
    throw "No se pudieron compilar las vistas con nivel Java $NivelJava."
}
Write-Output "$($vistas.Count) vistas compiladas sin errores con nivel Java $NivelJava."
