document.querySelectorAll('.padron-edicion').forEach(formulario => {
    formulario.addEventListener('submit', evento => {
        if (formulario.dataset.enviando) { evento.preventDefault(); return; }
        formulario.dataset.enviando = 'true';
        const boton = formulario.querySelector('button[type="submit"]');
        boton.disabled = true;
        boton.textContent = 'Guardando…';
        formulario.setAttribute('aria-busy', 'true');
    });
});
window.addEventListener('pageshow', () => {
    document.querySelectorAll('.padron-edicion').forEach(formulario => {
        delete formulario.dataset.enviando;
        formulario.removeAttribute('aria-busy');
        const boton = formulario.querySelector('button[type="submit"]');
        boton.disabled = false;
        boton.textContent = 'Guardar cambios';
    });
});
