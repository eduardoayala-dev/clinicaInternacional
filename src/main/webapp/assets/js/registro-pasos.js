(() => {
    'use strict';
    const form = document.getElementById('registerForm');
    if (!form) return;
    const pasos = [...form.querySelectorAll('.registro-paso')];
    const siguiente = document.getElementById('registroContinuar');
    const atras = document.getElementById('registroAtras');
    const cancelar = document.getElementById('registroCancelar');
    const enviar = document.getElementById('btnSubmitReg');
    const titulo = document.getElementById('registroPasoTitulo');
    const detalle = document.getElementById('registroPasoDetalle');
    const progreso = document.querySelector('.registro-progreso-barras');
    const error = document.getElementById('registroError');
    const password = document.getElementById('regPwd');
    const confirmacion = document.getElementById('regPwdConfirm');
    const documento = document.getElementById('regDni');
    const coincidencia = document.getElementById('pwdMatchMsg');
    const nombres = ['Datos personales', 'Contacto y ubicación', 'Credenciales de acceso', 'Salud y cobertura'];
    const detalles = ['Identificación', 'Notificaciones y citas', 'Portal del paciente', 'Información del paciente'];
    const reducirMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)');
    let actual = 0;
    let cambiando = false;
    form.noValidate = true;
    form.classList.add('registro-listo');

    function mostrarError(mensaje) {
        error.textContent = mensaje;
        error.hidden = !mensaje;
    }

    function validarPassword() {
        let mensaje = '';
        if (password.value && !/^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_\-]).{8,}$/.test(password.value)) {
            mensaje = 'Usa al menos 8 caracteres con mayúscula, minúscula, número y un símbolo indicado.';
        } else if (documento.value && password.value.includes(documento.value)) {
            mensaje = 'La contraseña no debe contener tu número de documento.';
        }
        password.setCustomValidity(mensaje);
        confirmacion.setCustomValidity(confirmacion.value && password.value !== confirmacion.value ? 'Las contraseñas no coinciden.' : '');
        coincidencia.hidden = !(password.value && confirmacion.value && password.value === confirmacion.value);
        coincidencia.style.display = coincidencia.hidden ? 'none' : 'flex';
    }

    function primerInvalido(paso) {
        validarPassword();
        return [...paso.querySelectorAll('input,select')].find(campo => !campo.disabled && (
            !campo.checkValidity() || (campo.required && !campo.value.trim())
        ));
    }

    function avisarInvalido(campo) {
        if (campo.readOnly && !campo.value.trim()) {
            mostrarError('Espera a que termine la consulta del documento o completa los nombres cuando se habiliten.');
            documento.focus();
        } else {
            campo.focus();
            campo.reportValidity();
        }
    }

    function mostrar(indice, animar = true) {
        actual = indice;
        pasos.forEach((paso, i) => { paso.hidden = i !== actual; });
        titulo.textContent = `Paso ${actual + 1} de ${pasos.length} · ${nombres[actual]}`;
        detalle.textContent = detalles[actual];
        progreso.setAttribute('aria-valuenow', String(actual + 1));
        progreso.setAttribute('aria-valuetext', titulo.textContent);
        [...progreso.children].forEach((barra, i) => barra.classList.toggle('completo', i <= actual));
        atras.hidden = actual === 0;
        cancelar.hidden = actual !== 0;
        siguiente.hidden = actual === pasos.length - 1;
        enviar.hidden = actual !== pasos.length - 1;
        mostrarError('');
        document.getElementById('formPane').scrollTop = 0;
        if (animar && !reducirMovimiento.matches) {
            pasos[actual].animate([{opacity: 0, transform: 'translateY(12px)'}, {opacity: 1, transform: 'translateY(0)'}], {duration: 280, easing: 'cubic-bezier(.16,1,.3,1)'});
        }
    }

    async function cambiar(indice) {
        if (cambiando) return;
        cambiando = true;
        siguiente.disabled = true;
        atras.disabled = true;
        try {
            if (!reducirMovimiento.matches) {
                const salida = pasos[actual].animate([{opacity: 1, transform: 'translateY(0)'}, {opacity: 0, transform: 'translateY(-6px)'}], {duration: 160, easing: 'ease-out'});
                await salida.finished;
            }
            mostrar(indice);
            pasos[actual].querySelector('input:not([readonly]):not(:disabled),select')?.focus({preventScroll: true});
        } finally {
            cambiando = false;
            siguiente.disabled = false;
            atras.disabled = false;
        }
    }

    function continuar() {
        const invalido = primerInvalido(pasos[actual]);
        if (invalido) avisarInvalido(invalido);
        else if (actual < pasos.length - 1) void cambiar(actual + 1);
    }

    siguiente.addEventListener('click', continuar);
    atras.addEventListener('click', () => { if (actual > 0) void cambiar(actual - 1); });
    form.addEventListener('input', () => { validarPassword(); mostrarError(''); });
    form.addEventListener('submit', event => {
        if (actual < pasos.length - 1 || cambiando) {
            event.preventDefault();
            event.stopImmediatePropagation();
            if (!cambiando) continuar();
            return;
        }
        for (let i = 0; i < pasos.length; i++) {
            const invalido = primerInvalido(pasos[i]);
            if (!invalido) continue;
            event.preventDefault();
            event.stopImmediatePropagation();
            mostrar(i, false);
            avisarInvalido(invalido);
            return;
        }
    }, true);
    const fecha = document.getElementById('regFechaNacimiento');
    const hoy = new Date();
    hoy.setMinutes(hoy.getMinutes() - hoy.getTimezoneOffset());
    fecha.max = hoy.toISOString().slice(0, 10);
    mostrar(0, false);
})();
