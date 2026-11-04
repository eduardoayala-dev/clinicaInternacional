(() => {
    'use strict';
    const sidebar = document.getElementById('patientNavigation');
    const trigger = document.querySelector('.patient-menu-toggle');
    const backdrop = document.querySelector('.patient-menu-backdrop');
    const close = sidebar?.querySelector('.patient-menu-close');
    const main = document.querySelector('main');
    if (!sidebar || !trigger || !backdrop || !close || !main) return;
    for (const region of document.querySelectorAll('.table-card-container,.week-days-grid')) {
        region.tabIndex = 0;
        region.setAttribute('role', 'region');
        region.setAttribute('aria-label', region.classList.contains('week-days-grid')
            ? 'Días disponibles de la semana' : 'Tabla de resultados');
    }
    const mobile = window.matchMedia('(max-width: 900px)');
    let open = false;

    function setOpen(value, restoreFocus = false) {
        open = mobile.matches && value;
        document.body.classList.toggle('patient-menu-open', open);
        trigger.setAttribute('aria-expanded', String(open));
        backdrop.hidden = !open;
        sidebar.inert = mobile.matches && !open;
        main.inert = open;
        if (open) {
            sidebar.setAttribute('role', 'dialog');
            sidebar.setAttribute('aria-modal', 'true');
            window.requestAnimationFrame(() => { if (open) close.focus({preventScroll: true}); });
        } else {
            sidebar.removeAttribute('role');
            sidebar.removeAttribute('aria-modal');
            if (restoreFocus) trigger.focus();
        }
    }

    trigger.addEventListener('click', () => setOpen(!open, true));
    close.addEventListener('click', () => setOpen(false, true));
    backdrop.addEventListener('click', () => setOpen(false, true));
    mobile.addEventListener('change', () => setOpen(false));
    sidebar.addEventListener('transitionend', event => {
        if (open && event.propertyName === 'transform') close.focus({preventScroll: true});
    });
    sidebar.addEventListener('click', event => {
        if (event.target.closest('a[href]') && mobile.matches) setOpen(false);
    });
    document.addEventListener('keydown', event => {
        if (!open) return;
        if (event.key === 'Escape') {
            event.preventDefault();
            setOpen(false, true);
        } else if (event.key === 'Tab') {
            const items = [...sidebar.querySelectorAll('a[href],button:not([disabled]),input:not([type="hidden"]):not([disabled])')]
                .filter(element => element.getClientRects().length > 0);
            const first = items[0], last = items.at(-1);
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault(); last?.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault(); first?.focus();
            }
        }
    });
    setOpen(false);
})();
