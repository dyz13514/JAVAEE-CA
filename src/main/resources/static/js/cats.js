(() => {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    if (window.matchMedia('(hover: hover) and (pointer: fine)').matches) {
        document.addEventListener('pointermove', event => {
            if (reducedMotion.matches) return;
            const control = event.target.closest('button, .btn, .primary-button, .secondary-button, .danger-button, .success-button, .app-sidebar nav a, .management-link');
            if (!control || control.matches(':disabled, [aria-disabled="true"]')) return;
            const box = control.getBoundingClientRect();
            control.style.setProperty('--light-x', `${event.clientX - box.left}px`);
            control.style.setProperty('--light-y', `${event.clientY - box.top}px`);
        });
    }
    document.querySelectorAll('.allowance-meter').forEach(meter => {
        const limit = Number(meter.dataset.limit);
        const remaining = Number(meter.dataset.remaining);
        const percentage = limit > 0 ? Math.max(0, Math.min(100, remaining / limit * 100)) : 0;
        meter.style.setProperty('--balance', `${percentage}%`);
        meter.setAttribute('aria-label', `${meter.dataset.label}: ${remaining} of ${limit} remaining`);
    });
})();

(() => {
    const links = document.querySelectorAll('[data-finance-route]');
    if (!links.length) return;
    const person = document.querySelector('select[name="employeeId"]');
    const year = document.querySelector('input[name="entitlementYear"], input[name="year"]');
    const update = () => links.forEach(link => {
        const url = new URL(link.dataset.financeRoute, window.location.origin);
        if (person?.value) url.searchParams.set('employeeId', person.value);
        if (year?.value) url.searchParams.set(link.dataset.financeRoute === '/entitlements' ? 'entitlementYear' : 'year', year.value);
        link.href = url.pathname + url.search;
    });
    person?.addEventListener('change', update);
    year?.addEventListener('input', update);
    update();
})();
