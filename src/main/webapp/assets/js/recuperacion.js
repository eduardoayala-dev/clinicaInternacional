(() => {
    'use strict';
    function preparar() {
        const form = document.getElementById('restablecerForm');
        if (form) {
            const nueva = document.getElementById('nuevaPassword');
            const confirmar = document.getElementById('confirmarPassword');
            form.addEventListener('submit', event => {
                confirmar.setCustomValidity(nueva.value === confirmar.value ? '' : 'Las contraseñas no coinciden.');
                nueva.setCustomValidity(new TextEncoder().encode(nueva.value).length <= 72 ? '' : 'La contraseña excede el máximo de 72 bytes.');
                if (!form.reportValidity()) event.preventDefault();
            });
            confirmar.addEventListener('input', () => confirmar.setCustomValidity(''));
            nueva.addEventListener('input', () => nueva.setCustomValidity(''));
        }
        for (const formulario of document.querySelectorAll('.contenido-seguridad form')) {
            if (formulario.closest('[data-recuperacion-inline]')) continue;
            formulario.addEventListener('submit', event => {
                if (event.defaultPrevented) return;
                const boton = formulario.querySelector('button[type="submit"]');
                boton.dataset.textoOriginal = boton.textContent;
                boton.disabled = true;
                boton.textContent = 'Procesando…';
            });
        }
    }
    preparar();
    window.addEventListener('recuperacion:renderizada', preparar);
    window.addEventListener('pageshow', () => {
        for (const boton of document.querySelectorAll('button[data-texto-original]')) {
            boton.disabled = false;
            boton.textContent = boton.dataset.textoOriginal;
        }
    });
})();
