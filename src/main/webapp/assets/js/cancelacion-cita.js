(() => {
    const formulario = document.getElementById('formCancelarCita');
    if (!formulario) return;
    const confirmar = formulario.querySelector('button[type="submit"]');
    if (!confirmar) return;
    const texto = confirmar.textContent;
    let enviando = false;

    formulario.addEventListener('submit', evento => {
        if (evento.defaultPrevented) return;
        if (enviando) {
            evento.preventDefault();
            return;
        }
        enviando = true;
        formulario.setAttribute('aria-busy', 'true');
        formulario.querySelectorAll('button').forEach(boton => { boton.disabled = true; });
        confirmar.textContent = 'Cancelando…';
    });

    window.addEventListener('pageshow', () => {
        enviando = false;
        formulario.removeAttribute('aria-busy');
        formulario.querySelectorAll('button').forEach(boton => { boton.disabled = false; });
        confirmar.textContent = texto;
    });
})();
