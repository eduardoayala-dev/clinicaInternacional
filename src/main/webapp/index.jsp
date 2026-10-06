<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // Redirige automáticamente a la página de login
    response.sendRedirect(request.getContextPath() + "/login");
%>
