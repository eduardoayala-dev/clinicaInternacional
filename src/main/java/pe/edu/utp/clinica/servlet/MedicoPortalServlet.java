package pe.edu.utp.clinica.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.edu.utp.clinica.api.ApiJson;
import pe.edu.utp.clinica.api.ApiResponse;
import pe.edu.utp.clinica.dao.AgendaException;
import pe.edu.utp.clinica.dao.CatalogoDatosException;
import pe.edu.utp.clinica.dao.CitaDAO;
import pe.edu.utp.clinica.model.Usuario;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

abstract class MedicoPortalServlet extends HttpServlet {
    protected transient CitaDAO citas;
    protected record Contexto(Usuario usuario,int medico,Map<String,Object> perfil){}
    protected CitaDAO crearCitaDAO(){return new CitaDAO();}
    protected Clock reloj(){return Clock.system(ZoneId.of("America/Lima"));}
    protected String metodosPermitidos(){return "GET";}
    @Override public void init(){citas=crearCitaDAO();}
    protected abstract void mostrar(HttpServletRequest request,HttpServletResponse response,Contexto contexto)throws IOException,ServletException;
    protected void ejecutar(HttpServletRequest request,HttpServletResponse response,Contexto contexto)throws IOException{
        throw new AgendaException(405,"METODO_NO_PERMITIDO","Esta pantalla solo permite consultas.");
    }
    @Override protected final void doGet(HttpServletRequest request,HttpServletResponse response){atender(request,response,false);}
    @Override protected final void doPost(HttpServletRequest request,HttpServletResponse response){atender(request,response,true);}
    private void atender(HttpServletRequest request,HttpServletResponse response,boolean escritura){
        try{
            var session=request.getSession(false);
            Usuario usuario=session==null?null:(Usuario)session.getAttribute("usuarioLogueado");
            if(usuario==null || !"MEDICO".equals(usuario.getRol())) throw new AgendaException(403,"ACCESO_DENEGADO","Acceso exclusivo para médicos.");
            var perfil=citas.obtenerPerfilPortalMedico(usuario.getIdUsuario());
            Contexto contexto=new Contexto(usuario,(Integer)perfil.get("idMedico"),perfil);
            request.setAttribute("usuario",usuario);request.setAttribute("detallesMedico",perfil);
            request.setAttribute("nombreMedico",perfil.get("nombreMedico"));
            request.setAttribute("especialidadMedico",perfil.get("especialidad"));request.setAttribute("sedeMedico",perfil.get("sedeNombre"));
            if(!escritura && (request.getParameter("accion")!=null || request.getParameter("action")!=null)) throw new AgendaException(405,"METODO_NO_PERMITIDO","Utiliza POST para modificar una cita.");
            if(escritura) ejecutar(request,response,contexto);else mostrar(request,response,contexto);
        }catch(AgendaException e){error(request,response,e.status(),e.code(),e.getMessage());}
        catch(CatalogoDatosException e){error(request,response,503,"DATOS_NO_DISPONIBLES","No se pudieron consultar los datos. Inténtalo más tarde.");}
        catch(IOException e){if(!response.isCommitted()) response.setStatus(500);}
        catch(ServletException|RuntimeException e){
            java.util.logging.Logger.getLogger(getClass().getName()).warning("No se pudo completar la operación del portal médico: "+e.getClass().getSimpleName());
            error(request,response,500,"ERROR_INTERNO","No se pudo completar la operación.");
        }
    }
    protected static boolean json(HttpServletRequest request){String accept=request.getHeader("Accept");return accept!=null && accept.contains("application/json");}
    private void error(HttpServletRequest request,HttpServletResponse response,int status,String code,String message){
        if(response.isCommitted()) return;
        try{
            if(status==405) response.setHeader("Allow",metodosPermitidos());
            if(json(request)) ApiJson.write(response,status,new ApiResponse<>(code,message,null));
            else{response.setStatus(status);response.setContentType("text/plain;charset=UTF-8");response.getWriter().write(message);}
        }catch(IOException e){if(!response.isCommitted()) response.setStatus(500);}
    }
    protected LocalDate fecha(HttpServletRequest request){
        String value=request.getParameter("fecha");
        if(value==null) return LocalDate.now(reloj());
        try{
            if(!value.matches("\\d{4}-\\d{2}-\\d{2}")) throw new java.time.DateTimeException("Formato de fecha");
            LocalDate date=LocalDate.parse(value);
            if(date.getYear()<1900 || date.getYear()>9999) throw new java.time.DateTimeException("Rango de fecha");
            return date;
        }catch(java.time.DateTimeException e){throw new AgendaException(400,"FECHA_INVALIDA","Selecciona una fecha válida.");}
    }
    protected static int positivo(String value){
        try{
            if(value==null || !value.matches("[0-9]+")) throw new NumberFormatException();
            int id=Integer.parseInt(value);if(id<=0) throw new NumberFormatException();return id;
        }catch(NumberFormatException e){throw new AgendaException(400,"IDENTIFICADOR_INVALIDO","El identificador de la cita no es válido.");}
    }
    protected void agenda(HttpServletRequest request,Contexto contexto,LocalDate fecha){
        var agenda=citas.obtenerAgendaMedico(contexto.medico(),fecha);
        int atendidos=0,enSala=0,pendientes=0,inasistencias=0;
        Map<String,Object> pacienteEnSala=null;
        for(var cita:agenda){
            switch(String.valueOf(cita.get("estado"))){
                case "ATENDIDA"->atendidos++;
                case "EN_SALA"->{enSala++;if(pacienteEnSala==null) pacienteEnSala=cita;}
                case "CONFIRMADA","SOLICITADA"->pendientes++;
                case "NO_ASISTIO"->inasistencias++;
                default->{}
            }
        }
        request.setAttribute("agenda",agenda);request.setAttribute("fechaSeleccionada",fecha.toString());
        request.setAttribute("fechaActual",fecha.toString());request.setAttribute("fechaHoyStr",fecha.toString());
        request.setAttribute("totalCitas",agenda.size());request.setAttribute("totalAsignados",agenda.size());
        request.setAttribute("atendidos",atendidos);request.setAttribute("enEspera",enSala);request.setAttribute("pendientes",pendientes);
        request.setAttribute("pendientesRestantes",pendientes+enSala);request.setAttribute("inasistencias",inasistencias);
        request.setAttribute("pacienteEnSala",pacienteEnSala);
    }
}
