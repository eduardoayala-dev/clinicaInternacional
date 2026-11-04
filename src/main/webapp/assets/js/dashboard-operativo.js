(() => {
    'use strict';
    const panel=document.getElementById('panelOperativo');
    if(!panel)return;
    const intervalo=300000;
    let cargando=false;
    let controlador=null;
    let canal=null;
    let cambioPendiente=false;
    const firma=()=>new URLSearchParams(new FormData(document.getElementById('filtrosOperativo'))).toString();
    let firmaInicial=firma();
    const aviso=mensaje=>{document.getElementById('estadoActualizacionOperativa').textContent=mensaje;};
    async function actualizar(automatica) {
        if(cargando){cambioPendiente=true;return;}
        if(automatica && document.hidden)return;
        if(firma()!==firmaInicial){aviso('Tienes filtros sin consultar. Pulsa Consultar para aplicarlos.');return;}
        const form=document.getElementById('filtrosOperativo');
        if(automatica && form.contains(document.activeElement))return;
        cargando=true;panel.setAttribute('aria-busy','true');aviso('Actualizando datos…');
        controlador=new AbortController();
        const tiempo=setTimeout(()=>controlador.abort(),12000);
        try {
            const headers={Accept:'text/html','X-Requested-With':'XMLHttpRequest'};
            if(automatica)headers['X-Actualizacion-Automatica']='true';
            const respuesta=await fetch(window.location.href,{credentials:'same-origin',headers,signal:controlador.signal,cache:'no-store'});
            if(respuesta.redirected || respuesta.status===401 || respuesta.status===403)throw new Error('Tu sesión cambió o venció. Vuelve a ingresar para actualizar.');
            if(!respuesta.ok)throw new Error('No se pudo actualizar. Se conservan los datos de la última consulta.');
            const html=new DOMParser().parseFromString(await respuesta.text(),'text/html');
            const nuevo=html.getElementById('panelOperativo');
            if(!nuevo)throw new Error('No se pudo verificar la actualización. Se conservan los datos anteriores.');
            if(firma()!==firmaInicial || (automatica && form.contains(document.activeElement))){aviso('Actualización pospuesta para conservar tus filtros.');return;}
            const scroll=panel.scrollTop;
            panel.replaceChildren(...nuevo.childNodes);panel.scrollTop=scroll;firmaInicial=firma();
            aviso('Datos actualizados · Actualización en vivo con respaldo cada 5 minutos.');
            globalThis.lucide?.createIcons();
        } catch(error) {
            aviso(error.name==='AbortError'?'La actualización tardó demasiado. Se conservan los datos anteriores.':error.message);
        } finally {
            clearTimeout(tiempo);cargando=false;controlador=null;panel.removeAttribute('aria-busy');
            if(cambioPendiente){cambioPendiente=false;queueMicrotask(()=>void actualizar(true));}
        }
    }
    panel.addEventListener('click',event=>{if(event.target.closest('#actualizarOperativo'))actualizar(false).catch(()=>aviso('No se pudo iniciar la actualización.'));});
    function conectar() {
        if(canal || document.hidden || !globalThis.EventSource)return;
        canal=new EventSource(panel.dataset.eventosUrl);
        canal.addEventListener('cambio',()=>void actualizar(true));
        canal.addEventListener('sincronizar',()=>void actualizar(true));
        canal.addEventListener('open',()=>aviso('Actualización en vivo conectada'));
        canal.addEventListener('error',()=>aviso('Reconectando · Respaldo automático cada 5 minutos'));
    }
    document.addEventListener('visibilitychange',()=>{
        canal?.close();canal=null;
        if(!document.hidden){conectar();void actualizar(true);}
    });
    conectar();
    const temporizador=setInterval(()=>actualizar(true).catch(()=>aviso('No se pudo iniciar la actualización.')),intervalo);
    window.addEventListener('pagehide',()=>{clearInterval(temporizador);canal?.close();if(controlador)controlador.abort();},{once:true});
})();
