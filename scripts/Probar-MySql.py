from pathlib import Path
import subprocess,os,uuid,time,sys,argparse
parser=argparse.ArgumentParser(description='Pruebas MySQL en un contenedor temporal aislado.')
parser.add_argument('--solo-sesiones',action='store_true')
parser.add_argument('--solo-recuperacion',action='store_true')
parser.add_argument('--solo-correos',action='store_true')
parser.add_argument('--solo-horarios',action='store_true')
argumentos=parser.parse_args()
root=Path(__file__).resolve().parents[1]; tag=uuid.uuid4().hex[:12]; name='clinica-pr-mysql-'+tag; schema='clinica_agendas_test_'+tag
assert name.startswith('clinica-pr-mysql-') and schema.startswith('clinica_agendas_test_')
env=dict(os.environ); password=uuid.uuid4().hex+uuid.uuid4().hex
env.update(MYSQL_ROOT_PASSWORD=password,MYSQL_DATABASE=schema,MYSQL_USER='clinica_app',MYSQL_PASSWORD=password,AGENDA_MYSQL_URL='jdbc:mysql://'+name+':3306/'+schema+'?sslMode=REQUIRED&serverTimezone=America/Lima&characterEncoding=UTF-8&allowPublicKeyRetrieval=false',AGENDA_MYSQL_USER='clinica_app',AGENDA_MYSQL_PASSWORD=password)
def run(args,**kw):return subprocess.run(['docker']+args,env=env,**kw)
started=False; result=1
try:
    run(['run','-d','--name',name,'--network','clinicainternacional_default','--tmpfs','/var/lib/mysql:rw,nosuid,size=512m','-e','MYSQL_ROOT_PASSWORD','-e','MYSQL_DATABASE','-e','MYSQL_USER','-e','MYSQL_PASSWORD','mysql:8.0','--require-secure-transport=ON'],stdout=subprocess.DEVNULL,stderr=subprocess.PIPE,check=True);started=True
    for attempt in range(60):
        probe=run(['exec',name,'sh','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root --protocol=tcp -h127.0.0.1 -e "SELECT 1"'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        if probe.returncode==0:break
        time.sleep(1)
    else:raise RuntimeError('MySQL aislado no inicio')
    suites='MySqlPruebasTest,SesionesMySqlTest' if argumentos.solo_sesiones else 'AgendaCitaMySqlTest,AtencionCitaMySqlTest,AdmisionCitaMySqlTest,HistorialMedicoMySqlTest,OfertasMySqlTest,MedicoPortalMySqlTest,MySqlPruebasTest,SesionesMySqlTest,RecuperacionMySqlTest,CorreoCitaMySqlTest,PropuestaHorarioMySqlTest'
    if argumentos.solo_recuperacion: suites='MySqlPruebasTest,RecuperacionMySqlTest'
    if argumentos.solo_correos: suites='MySqlPruebasTest,CorreoCitaMySqlTest'
    if argumentos.solo_horarios: suites='MySqlPruebasTest,PropuestaHorarioMySqlTest'
    shell='mkdir -p /tmp/revision-pr && cp /work/pom.xml /tmp/revision-pr/ && cp -r /work/src /work/sql /tmp/revision-pr/ && cd /tmp/revision-pr && mvn -B -Dtest='+suites+' -DargLine=-Xmx256m -Dsurefire.reportNameSuffix=mysql test; resultado=$?; if [ -d target/surefire-reports ]; then cp target/surefire-reports/* /work/target/pruebas-mysql/reports/; fi; exit "$resultado"'
    (root/'target/pruebas-mysql/reports').mkdir(parents=True,exist_ok=True)
    with (root/'target/pruebas-mysql/ejecucion.log').open('wb') as log:
        result=run(['run','--rm','--network','clinicainternacional_default','--mount','type=bind,source='+str(root)+',target=/work','--mount','type=bind,source='+str(Path.home()/'.m2/repository')+',target=/root/.m2/repository','-e','AGENDA_MYSQL_URL','-e','AGENDA_MYSQL_USER','-e','AGENDA_MYSQL_PASSWORD','-e','MAVEN_OPTS=-Xmx256m','maven:3.9-eclipse-temurin-17','sh','-c',shell],stdout=log,stderr=subprocess.STDOUT).returncode
    print('Pruebas MySQL aislado: salida',result)
finally:
    if started:run(['rm','-f',name],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,check=True)
    print('Contenedor temporal retirado; contenedores y volumen de clinica conservados.')
sys.exit(result)
