package pe.edu.utp.clinica.repository;

import jakarta.persistence.criteria.*;

import pe.edu.utp.clinica.persistence.entity.*;

import java.time.LocalDate;

final class PrioridadClinicaExpression {
    private PrioridadClinicaExpression() {}

    private static Predicate evaluada(
            CriteriaBuilder cb,
            Root<CitaEntidad> c,
            Root<AtencionPrioridadEntidad> a,
            Expression<Integer> paciente,
            Expression<Integer> especialidad) {
        return cb.and(
                cb.equal(a.get("id"), c.get("id")),
                cb.equal(a.get("medico"), c.get("medico").get("id")),
                cb.equal(c.get("estado"), "ATENDIDA"),
                cb.isNotNull(a.get("fin")),
                c.get("prioridad").in("ALTA", "MEDIA", "BAJA"),
                cb.equal(c.get("paciente"), paciente),
                cb.equal(a.get("especialidad"), especialidad));
    }

    static Expression<String> nivel(
            CriteriaBuilder cb,
            CommonAbstractCriteria consulta,
            Expression<Integer> paciente,
            Expression<Integer> especialidad,
            LocalDate fecha) {
        var q = consulta.subquery(String.class);
        var c = q.from(CitaEntidad.class);
        var a = q.from(AtencionPrioridadEntidad.class);
        var posteriores = q.subquery(Integer.class);
        var otra = posteriores.from(CitaEntidad.class);
        var atencion = posteriores.from(AtencionPrioridadEntidad.class);
        posteriores
                .select(cb.literal(1))
                .where(
                        evaluada(cb, otra, atencion, paciente, especialidad),
                        cb.or(
                                cb.greaterThan(atencion.get("fin"), a.get("fin")),
                                cb.and(
                                        cb.equal(atencion.get("fin"), a.get("fin")),
                                        cb.greaterThan(otra.get("id"), c.get("id")))));
        var controles = q.subquery(Integer.class);
        var seguimiento = controles.from(SeguimientoEntidad.class);
        var control = seguimiento.join("control");
        controles
                .select(cb.literal(1))
                .where(
                        cb.equal(seguimiento.get("id"), c.get("id")),
                        cb.equal(control.get("paciente"), c.get("paciente")),
                        cb.equal(control.get("estado"), "ATENDIDA"));
        var vigente =
                cb.and(
                        cb.or(
                                cb.isNull(c.get("recomendada")),
                                cb.greaterThanOrEqualTo(c.get("recomendada"), fecha)),
                        cb.not(cb.exists(controles)));
        q.select(
                        cb.<String>selectCase()
                                .when(vigente, c.<String>get("prioridad"))
                                .otherwise(cb.nullLiteral(String.class)))
                .where(evaluada(cb, c, a, paciente, especialidad), cb.not(cb.exists(posteriores)));
        return q;
    }
}
