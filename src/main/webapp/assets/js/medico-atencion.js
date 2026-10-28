globalThis.lucide?.createIcons();

        function setPrioridad(valor, elemento) {
            if (!formularioEditable()) return;
            document.querySelectorAll('.med-prio-card').forEach(c => c.classList.remove('active'));
            elemento.classList.add('active');
            document.getElementById('prioridadInput').value = valor;
        }

        function actualizarContador(textarea) {
            const count = textarea.value.length;
            document.getElementById('charCounter').textContent = count + ' / 200 caracteres';
        }

        function actualizarBadgeMeses() {
            const fecha = document.getElementById('fechaProximaInput');
            const badge = document.getElementById('badgeMeses');
            if (fecha && badge) badge.textContent = fecha.value || 'Sin fecha de seguimiento';
        }

        function formularioEditable() {
            const grupo = document.querySelector('#formAtencion fieldset');
            return Boolean(grupo && !grupo.disabled);
        }

        function restablecerFormulario() {
            if (!formularioEditable()) return;
            const txt = document.getElementById('indicacionesTextarea');
            const fInput = document.getElementById('fechaProximaInput');
            if (txt) {
                txt.value = txt.defaultValue;
                actualizarContador(txt);
            }
            if (fInput) {
                fInput.value = fInput.defaultValue;
                actualizarBadgeMeses();
            }
        }

        document.addEventListener('DOMContentLoaded', () => {
            const textarea = document.getElementById('indicacionesTextarea');
            if (textarea) actualizarContador(textarea);
            actualizarBadgeMeses();
        });
