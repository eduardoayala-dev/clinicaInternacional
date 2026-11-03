(() => {
    'use strict';
    function iniciar(doc, win) {
        const formularios = [...doc.querySelectorAll('[data-importacion-motivos]')];
        const error = doc.getElementById('importacionArchivoError');
        function reiniciar() {
            for (const form of formularios) {
                delete form.dataset.enviando;
                form.removeAttribute('aria-busy');
                for (const boton of form.querySelectorAll('button[type="submit"]')) {
                    if (boton.dataset.textoInicial) boton.textContent = boton.dataset.textoInicial;
                    boton.disabled = false;
                }
            }
        }
        for (const form of formularios) {
            form.addEventListener('submit', event => {
                if (form.dataset.enviando) { event.preventDefault(); return; }
                const campo = form.querySelector('input[type="file"]');
                if (campo) {
                    const archivo = campo.files?.[0];
                    if (!archivo || !/\.xlsx$/i.test(archivo.name) || archivo.size < 1 || archivo.size > 1048576) {
                        event.preventDefault();
                        if (error) { error.hidden = false; error.textContent = 'Selecciona un archivo XLSX de hasta 1 MB que no esté vacío.'; }
                        campo.setAttribute('aria-invalid', 'true'); campo.focus(); return;
                    }
                    campo.removeAttribute('aria-invalid');
                    if (error) error.hidden = true;
                }
                form.dataset.enviando = 'true'; form.setAttribute('aria-busy', 'true');
                for (const boton of form.querySelectorAll('button[type="submit"]')) {
                    boton.dataset.textoInicial = boton.textContent;
                    boton.textContent = campo ? 'Revisando archivo…' : 'Importando motivos…'; boton.disabled = true;
                }
            });
        }
        win.addEventListener('pageshow', reiniciar);
        return {reiniciar};
    }
    if (typeof module !== 'undefined') module.exports = {iniciar};
    else iniciar(document, window);
})();
