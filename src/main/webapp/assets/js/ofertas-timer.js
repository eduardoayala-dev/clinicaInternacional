(() => {
    'use strict';
    function iniciar(segundos, destino) {
        const timer = document.getElementById('countdownTimerDigits');
        if (!timer) return;
        const inicio = performance.now();
        const duracion = Number.isFinite(segundos) ? Math.max(0, segundos) : 0;
        let intervalo;
        function actualizar() {
            const restante = Math.max(0, Math.ceil(duracion - (performance.now() - inicio) / 1000));
            const minutos = String(Math.floor(restante / 60)).padStart(2, '0');
            const segundos = String(restante % 60).padStart(2, '0');
            timer.textContent = minutos + ':' + segundos + ' min';
            if (restante === 0) {
                clearInterval(intervalo);
                document.querySelectorAll('#cardOfertaActiva button').forEach(button => { button.disabled = true; });
                window.location.replace(destino);
            }
            return restante;
        }
        if (actualizar() > 0) intervalo = setInterval(actualizar, 1000);
    }
    globalThis.ClinicaOfertas = Object.freeze({iniciar});
})();
