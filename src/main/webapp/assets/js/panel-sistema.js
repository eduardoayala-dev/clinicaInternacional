document.querySelectorAll('[data-exportar-tabla]').forEach(boton => {
    boton.addEventListener('click', () => {
        const tabla=document.getElementById(boton.dataset.exportarTabla);
        if(!tabla)return;
        const celda=valor=>{
            let texto=valor.replace(/\s+/g,' ').trim();
            if(/^[=+@-]/.test(texto))texto="'"+texto;
            return '"'+texto.replaceAll('"','""')+'"';
        };
        const filas=Array.from(tabla.rows,fila=>Array.from(fila.cells,c=>celda(c.textContent)).join(';')).join('\r\n');
        const url=URL.createObjectURL(new Blob(['\uFEFF'+filas],{type:'text/csv;charset=utf-8'}));
        const enlace=document.createElement('a');enlace.href=url;enlace.download='indicadores-sedes.csv';enlace.click();URL.revokeObjectURL(url);
    });
});
