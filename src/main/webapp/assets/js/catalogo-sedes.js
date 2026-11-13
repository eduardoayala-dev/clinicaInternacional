document.querySelectorAll('.sedes-formulario').forEach(form => {
    form.addEventListener('submit', evento => {
        if(form.dataset.enviando){evento.preventDefault();return;}
        form.dataset.enviando='true';form.setAttribute('aria-busy','true');
        const boton=form.querySelector('button[type="submit"]');boton.dataset.texto=boton.textContent;boton.disabled=true;boton.textContent='Guardando…';
    });
});
window.addEventListener('pageshow',()=>document.querySelectorAll('.sedes-formulario').forEach(form=>{
    delete form.dataset.enviando;form.removeAttribute('aria-busy');const boton=form.querySelector('button[type="submit"]');boton.disabled=false;if(boton.dataset.texto)boton.textContent=boton.dataset.texto;
}));

document.querySelectorAll('[data-abrir-dialog]').forEach(boton=>boton.addEventListener('click',()=>document.getElementById(boton.dataset.abrirDialog)?.showModal()));
document.querySelectorAll('[data-cerrar-dialog]').forEach(boton=>boton.addEventListener('click',()=>boton.closest('dialog')?.close()));
let paginaSedes=1;
const filasSedes=Array.from(document.querySelectorAll('.sede-fila'));
function filtrarSedes(reiniciar=false){
    if(reiniciar)paginaSedes=1;
    const texto=document.getElementById('buscarSede').value.trim().toLocaleLowerCase('es');
    const estado=document.getElementById('estadoSede').value;
    const visibles=filasSedes.filter(f=>(!texto||f.dataset.busqueda.toLocaleLowerCase('es').includes(texto))&&(!estado||f.dataset.estado===estado));
    const paginas=Math.max(1,Math.ceil(visibles.length/8));paginaSedes=Math.min(paginaSedes,paginas);
    filasSedes.forEach(f=>f.hidden=true);visibles.slice((paginaSedes-1)*8,paginaSedes*8).forEach(f=>f.hidden=false);
    document.getElementById('sedesVacias').hidden=visibles.length>0;
    document.getElementById('cantidadSedes').textContent=visibles.length+' de '+filasSedes.length+' sedes';
    document.getElementById('paginaSedes').textContent='Página '+paginaSedes+' de '+paginas;
    document.getElementById('sedesAnterior').disabled=paginaSedes<=1;document.getElementById('sedesSiguiente').disabled=paginaSedes>=paginas;
}
document.getElementById('buscarSede').addEventListener('input',()=>filtrarSedes(true));
document.getElementById('estadoSede').addEventListener('change',()=>filtrarSedes(true));
document.getElementById('sedesAnterior').addEventListener('click',()=>{paginaSedes--;filtrarSedes();});
document.getElementById('sedesSiguiente').addEventListener('click',()=>{paginaSedes++;filtrarSedes();});
filtrarSedes();
document.querySelectorAll('[data-nombre-sede]').forEach(campo=>{
    const form=campo.form;if(Number(form.elements.id.value)!==0)return;
    campo.addEventListener('input',()=>{
        const omitir=new Set(['SEDE','CLINICA','DE','DEL','Y']);
        const palabras=campo.value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toUpperCase().replace(/[^A-Z0-9 ]/g,' ').split(/ +/);
        const iniciales=palabras.filter(p=>p&&!omitir.has(p)).slice(0,10).map(p=>p[0]).join('');
        form.elements.codigo.value=iniciales?'SED-'+iniciales:'';
    });
});