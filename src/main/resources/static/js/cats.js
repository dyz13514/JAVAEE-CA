// Keep pointer feedback local to controls; touch and reduced-motion users get a quiet UI.
(() => {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    if (window.matchMedia('(hover: hover) and (pointer: fine)').matches) {
        document.querySelectorAll('button, .btn, .primary-button, .app-sidebar nav a, .management-link').forEach(control => {
            control.addEventListener('pointermove', event => {
                if (reducedMotion.matches) return;
                const box = control.getBoundingClientRect();
                control.style.setProperty('--light-x', `${event.clientX - box.left}px`);
                control.style.setProperty('--light-y', `${event.clientY - box.top}px`);
            });
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
