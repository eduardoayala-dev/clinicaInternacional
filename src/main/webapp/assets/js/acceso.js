(() => {
    'use strict';
    for (const button of document.querySelectorAll('[data-password-toggle]')) {
        button.addEventListener('click', () => {
            const input = document.getElementById(button.dataset.passwordToggle);
            if (!input) return;
            const mostrar = input.type === 'password';
            input.type = mostrar ? 'text' : 'password';
            button.setAttribute('aria-pressed', String(mostrar));
            button.title = mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña';
        });
    }
})();
