document.addEventListener('DOMContentLoaded', () => {
    const sidebar = document.getElementById('medSidebar');
    const toggle = document.querySelector('.med-menu-toggle');
    const main = document.querySelector('.med-main-wrapper');
    const mobile = globalThis.matchMedia('(max-width: 900px)');

    function menu(open, restoreFocus = false) {
        const expanded = mobile.matches && open;
        document.body.classList.toggle('med-menu-open', expanded);
        toggle?.setAttribute('aria-expanded', String(expanded));
        if (sidebar) sidebar.inert = mobile.matches && !expanded;
        if (main) main.inert = expanded;
        if (expanded) sidebar.querySelector('button, a')?.focus();
        else if (restoreFocus && toggle?.getClientRects().length) toggle.focus();
    }

    toggle?.addEventListener('click', () => menu(true));
    document.querySelectorAll('[data-close-menu]').forEach(button => {
        button.addEventListener('click', () => menu(false, true));
    });
    mobile.addEventListener('change', () => menu(false));
    menu(false);

    document.addEventListener('keydown', event => {
        if (!document.body.classList.contains('med-menu-open')) return;
        if (event.key === 'Escape') {
            event.preventDefault();
            menu(false, true);
            return;
        }
        if (event.key !== 'Tab') return;
        const controls = [...sidebar.querySelectorAll('a, button:not(:disabled)')];
        const first = controls[0];
        const last = controls.at(-1);
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last?.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first?.focus();
        }
    });

    function actualizarTablasDesplazables() {
        document.querySelectorAll('.med-table-card').forEach(region => {
            region.tabIndex = region.scrollWidth > region.clientWidth ? 0 : -1;
        });
    }
    actualizarTablasDesplazables();
    globalThis.addEventListener('resize', actualizarTablasDesplazables);
    globalThis.addEventListener('pageshow', actualizarTablasDesplazables);

    let pending = false;
    document.querySelectorAll('form[data-pending-label]').forEach(form => {
        form.addEventListener('submit', event => {
            if (event.defaultPrevented) return;
            if (pending) {
                event.preventDefault();
                return;
            }
            pending = true;
            document.querySelectorAll('form[data-pending-label] button[type="submit"]').forEach(button => {
                button.dataset.originalLabel = button.querySelector('.med-button-label')?.textContent || '';
                button.disabled = true;
            });
            const button = form.querySelector('button[type="submit"]');
            button?.classList.add('is-loading');
            const label = button?.querySelector('.med-button-label');
            if (label) label.textContent = form.dataset.pendingLabel;
            form.setAttribute('aria-busy', 'true');
        });
    });

    globalThis.addEventListener('pageshow', () => {
        pending = false;
        document.querySelectorAll('form[aria-busy="true"]').forEach(form => form.removeAttribute('aria-busy'));
        document.querySelectorAll('button[data-original-label]').forEach(button => {
            button.disabled = false;
            button.classList.remove('is-loading');
            const label = button.querySelector('.med-button-label');
            if (label) label.textContent = button.dataset.originalLabel;
            delete button.dataset.originalLabel;
        });
    });
});
