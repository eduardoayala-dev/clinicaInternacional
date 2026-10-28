const prioridadInicial = document.getElementById('prioridadInput')?.value || 'MEDIA';

function setPrioridad(valor, elemento) {
    if (!formularioEditable()) return;
    document.querySelectorAll('.med-prio-card').forEach(card => {
        card.classList.remove('active');
        card.setAttribute('aria-pressed', 'false');
    });
    elemento.classList.add('active');
    elemento.setAttribute('aria-pressed', 'true');
    document.getElementById('prioridadInput').value = valor;
}

function actualizarContador(textarea) {
    document.getElementById('charCounter').textContent = textarea.value.length + ' / 200 caracteres';
}

function actualizarBadgeMeses() {
    const fecha = document.getElementById('fechaProximaInput');
    const badge = document.getElementById('badgeMeses');
    if (!fecha || !badge) return;
    badge.textContent = fecha.value ? fecha.value.split('-').reverse().join('/') : 'Sin fecha de seguimiento';
}

function formularioEditable() {
    const grupo = document.querySelector('#formAtencion fieldset');
    return Boolean(grupo && !grupo.disabled);
}

function restablecerFormulario() {
    if (!formularioEditable()) return;
    const txt = document.getElementById('indicacionesTextarea');
    const fecha = document.getElementById('fechaProximaInput');
    if (txt) {
        txt.value = txt.defaultValue;
        actualizarContador(txt);
    }
    if (fecha) {
        fecha.value = fecha.defaultValue;
        actualizarBadgeMeses();
    }
    const tarjeta = document.querySelector('.med-prio-card[data-prioridad="' + prioridadInicial + '"]');
    if (tarjeta) setPrioridad(prioridadInicial, tarjeta);
}

document.addEventListener('DOMContentLoaded', () => {
    const textarea = document.getElementById('indicacionesTextarea');
    if (textarea) actualizarContador(textarea);
    actualizarBadgeMeses();
});
