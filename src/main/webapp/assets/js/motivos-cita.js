(() => {
    'use strict';
    function disponibles(catalogo, especialidad) {
        return catalogo.filter(m => String(m.idEspecialidad) === String(especialidad));
    }
    function poblar(select, catalogo, especialidad, seleccionado = '') {
        if (!select) return;
        const motivos = disponibles(catalogo, especialidad);
        select.replaceChildren();
        const inicial = new Option(motivos.length ? 'Selecciona un motivo' : 'No hay motivos disponibles', '');
        inicial.disabled = true;
        select.appendChild(inicial);
        motivos.forEach(m => select.appendChild(new Option(m.descripcion, String(m.idMotivo))));
        select.value = motivos.some(m => String(m.idMotivo) === String(seleccionado)) ? String(seleccionado) : '';
        select.disabled = motivos.length === 0;
    }
    function sugerido(catalogo, especialidad, idMotivo) {
        if (!idMotivo) return null;
        return disponibles(catalogo, especialidad).find(m => String(m.idMotivo) === String(idMotivo)) || null;
    }
    const api = Object.freeze({disponibles, poblar, sugerido});
    if (typeof module !== 'undefined' && module.exports) module.exports = api;
    globalThis.ClinicaMotivos = api;
})();
