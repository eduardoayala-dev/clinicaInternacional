(function () {
    'use strict';
    function iniciar(doc) {
        const exportar = doc.getElementById('exportarMedicos');
        if (exportar) exportar.addEventListener('click', () => {
            const filtros = new URLSearchParams(new FormData(doc.getElementById('formFiltros')));
            window.location.assign(exportar.dataset.exportarUrl + '?' + filtros.toString());
        });
        const cuerpo = doc.getElementById('padronMedicos');
        if (!cuerpo) return;
        const filas = Array.from(cuerpo.querySelectorAll('tr[data-medico]'));
        const anterior = doc.getElementById('medicosAnterior');
        const siguiente = doc.getElementById('medicosSiguiente');
        const paginaTexto = doc.getElementById('medicosPagina');
        const resumen = doc.getElementById('medicosResumen');
        const tamano = 10;
        const paginas = Math.max(1, Math.ceil(filas.length / tamano));
        let pagina = 1;
        function mostrar() {
            const inicio = (pagina - 1) * tamano;
            filas.forEach((fila, indice) => { fila.hidden = indice < inicio || indice >= inicio + tamano; });
            anterior.disabled = pagina === 1;
            siguiente.disabled = pagina === paginas;
            paginaTexto.textContent = 'Página ' + pagina + ' de ' + paginas;
            resumen.textContent = filas.length === 0 ? 'Sin médicos para los filtros seleccionados' :
                'Mostrando ' + (inicio + 1) + '–' + Math.min(inicio + tamano, filas.length) + ' de ' + filas.length + ' médicos';
        }
        anterior.addEventListener('click', () => { if (pagina > 1) { pagina--; mostrar(); } });
        siguiente.addEventListener('click', () => { if (pagina < paginas) { pagina++; mostrar(); } });
        mostrar();
    }
    if (typeof module !== 'undefined') module.exports = { iniciar };
    if (typeof document !== 'undefined') iniciar(document);
}());
