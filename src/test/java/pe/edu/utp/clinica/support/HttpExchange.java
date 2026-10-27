package pe.edu.utp.clinica.support;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.StringReader;
import java.io.BufferedReader;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Dobles HTTP sin contenedor ni dependencias adicionales. */
public class HttpExchange {
    public final Map<String, Object> attributes = new HashMap<>();
    public final Map<String, String> parameters = new HashMap<>();
    public final Map<String, String> headers = new HashMap<>();
    public final Map<String, String> responseHeaders = new HashMap<>();
    public final StringWriter body = new StringWriter();
    public String method = "POST";
    public String path = "/paciente/historial";
    public int status = 200;
    public String contentType;
    public String redirect;
    public String requestBody = "";
    public String requestContentType = "application/json";
    public String remoteAddr = "127.0.0.1";
    public HttpSession session = newSession();

    public HttpSession newSession() {
        Map<String, Object> data = new HashMap<>();
        String id = UUID.randomUUID().toString();
        return (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class}, (proxy, m, args) -> {
                    switch (m.getName()) {
                        case "getId": return id;
                        case "getAttribute": return data.get(args[0]);
                        case "setAttribute": data.put((String) args[0], args[1]); return null;
                        case "removeAttribute": data.remove(args[0]); return null;
                        case "invalidate": data.clear(); session = null; return null;
                        default: return null;
                    }
                });
    }

    public final HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
            HttpServletRequest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
            (proxy, m, args) -> {
                switch (m.getName()) {
                    case "getMethod": return method;
                    case "getReader": return new BufferedReader(new StringReader(requestBody));
                    case "getContentType": return requestContentType;
                    case "getRemoteAddr": return remoteAddr;
                    case "getServletPath": return path;
                    case "getRequestURI": return path;
                    case "getContextPath": return "";
                    case "getSession":
                        if (session == null && (args == null || Boolean.TRUE.equals(args[0]))) session = newSession();
                        return session;
                    case "getParameter": return parameters.get(args[0]);
                    case "getHeader": return headers.get(args[0]);
                    case "getAttribute": return attributes.get(args[0]);
                    case "setAttribute": attributes.put((String) args[0], args[1]); return null;
                    case "getDateHeader": return -1L;
                    default: return null;
                }
            });

    public final HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
            HttpServletResponse.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
            (proxy, m, args) -> {
                switch (m.getName()) {
                    case "setStatus": case "sendError": status = (Integer) args[0]; return null;
                    case "setContentType": contentType = (String) args[0]; return null;
                    case "setHeader": responseHeaders.put((String) args[0], (String) args[1]); return null;
                    case "sendRedirect": redirect = (String) args[0]; status = 302; return null;
                    case "getWriter": return new PrintWriter(body);
                    default: return null;
                }
            });
}
