document.addEventListener('DOMContentLoaded', () => {
    const turno = document.getElementById('medTurno');
    const filas = [...document.querySelectorAll('#medCitasDia tr[data-turno]')];
    const contador = document.getElementById('medCitasVisibles');
    const vacio = document.getElementById('medTurnoVacio');
    if (!turno || !contador || !vacio) return;

    const anterior = document.getElementById('medInicioAnterior');
    const siguiente = document.getElementById('medInicioSiguiente');
    const estado = document.getElementById('medInicioPagina');
    let pagina = 1;
    function filtrar() {
        const resultados = filas.filter(fila => turno.value === 'TODOS' || fila.dataset.turno === turno.value);
        const paginas = Math.max(1, Math.ceil(resultados.length / 6));
        pagina = Math.min(pagina, paginas);
        filas.forEach(fila => { fila.hidden = true; });
        resultados.slice((pagina - 1) * 6, pagina * 6).forEach(fila => { fila.hidden = false; });
        contador.textContent = `${resultados.length} ${resultados.length === 1 ? 'cita registrada' : 'citas registradas'}`;
        vacio.hidden = resultados.length > 0 || filas.length === 0;
        anterior.disabled = pagina === 1;
        siguiente.disabled = pagina === paginas;
        estado.textContent = `Página ${pagina} de ${paginas}`;
    }

    turno.addEventListener('change', () => { pagina = 1; filtrar(); });
    anterior.addEventListener('click', () => { pagina--; filtrar(); });
    siguiente.addEventListener('click', () => { pagina++; filtrar(); });
    globalThis.addEventListener('pageshow', filtrar);
    filtrar();
});
