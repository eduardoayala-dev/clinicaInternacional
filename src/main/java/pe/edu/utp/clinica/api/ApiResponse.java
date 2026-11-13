package pe.edu.utp.clinica.api;

public record ApiResponse<T>(String code, String message, T data) {}
