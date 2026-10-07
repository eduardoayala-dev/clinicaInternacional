package pe.edu.utp.clinica.dao;

import pe.edu.utp.clinica.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CitaDAO {

    public List<Map<String, Object>> obtenerEspecialidades() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_especialidad, nombre FROM especialidad WHERE estado = 'ACTIVO'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id_especialidad"));
                map.put("nombre", rs.getString("nombre"));
                lista.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<Map<String, Object>> obtenerSedes() {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT id_sede, nombre, direccion FROM sede WHERE estado = 'ACTIVO'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id_sede"));
                map.put("nombre", rs.getString("nombre"));
                map.put("direccion", rs.getString("direccion"));
                lista.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<Map<String, Object>> obtenerMedicosDisponibles(int idEspecialidad, int idSede) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT m.id_medico, u.nombres, u.apellidos, m.cmp " +
                     "FROM medico m " +
                     "INNER JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "WHERE m.id_especialidad = ? AND m.id_sede = ? AND m.estado = 'ACTIVO' AND u.activo = 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEspecialidad);
            ps.setInt(2, idSede);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("idMedico", rs.getInt("id_medico"));
                    map.put("nombreCompleto", rs.getString("nombres") + " " + rs.getString("apellidos"));
                    map.put("cmp", rs.getString("cmp"));
                    lista.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<Map<String, Object>> obtenerMedicosPorEspecialidad(int idEspecialidad) {
        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "SELECT m.id_medico, u.nombres, u.apellidos, m.cmp " +
                     "FROM medico m " +
                     "INNER JOIN usuario u ON m.id_usuario = u.id_usuario " +
                     "WHERE m.id_especialidad = ? AND m.estado = 'ACTIVO' AND u.activo = 1";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEspecialidad);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("idMedico", rs.getInt("id_medico"));
                    map.put("nombreCompleto", rs.getString("nombres") + " " + rs.getString("apellidos"));
                    map.put("cmp", rs.getString("cmp"));
                    lista.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

}
