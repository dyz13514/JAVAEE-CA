(() => {
    const page = document.querySelector('.login-page');
    if (!page) return;

    const tabs = page.querySelector('.role-tabs');
    const choices = [...tabs.querySelectorAll('a[data-role]')];
    const indicator = tabs.querySelector('.role-indicator');
    const form = page.querySelector('#login-form');
    const help = page.querySelector('.login-help');
    const error = page.querySelector('#login-error');
    const ambient = page.querySelector('.login-ambient');
    const trace = page.querySelector('.login-light-trace');
    const initialRole = page.dataset.loginRole;
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const gsap = window.gsap;
    let roleAnimation;

    function alignIndicator(animate = false) {
        const active = choices.find(choice => choice.dataset.role === page.dataset.loginRole);
        if (!active.offsetWidth) return;
        const width = active.offsetWidth;
        const x = active.offsetLeft;
        roleAnimation?.kill();
        const previousWidth = indicator.getBoundingClientRect().width || width;
        indicator.style.width = `${width}px`;
        tabs.classList.add('enhanced');
        if (!gsap || !animate || reducedMotion.matches) {
            if (gsap) gsap.set(indicator, { x, scaleX: 1 });
            else indicator.style.transform = `translateX(${x}px)`;
            return;
        }
        // Change width once, compensate with scale, then animate only transforms.
        gsap.set(indicator, { scaleX: previousWidth / width });
        roleAnimation = gsap.timeline()
            .to(indicator, { x, scaleX: 1.04, duration: .2, ease: 'power3.out' })
            .to(indicator, { scaleX: 1, duration: .08, ease: 'power2.out' });
    }

    function setRole(role, updateUrl = true, animate = true) {
        const selected = choices.find(choice => choice.dataset.role === role);
        if (!selected) return;
        page.dataset.loginRole = role;
        choices.forEach(choice => {
            const active = choice === selected;
            choice.classList.toggle('active', active);
            if (active) choice.setAttribute('aria-current', 'true');
            else choice.removeAttribute('aria-current');
        });
        form.action = selected.href;
        help.textContent = role === 'admin'
            ? 'Forgot your password? Contact your system maintainer.'
            : 'Forgot your password? Contact your administrator.';
        if (error) error.hidden = role !== initialRole;
        page.querySelector('#registration-entry').hidden = role === 'admin';
        if (updateUrl) window.history.replaceState(null, '', selected.href);
        alignIndicator(animate);
        const x = role === 'admin' ? 118 : 0;
        if (gsap) {
            gsap.to(ambient, { x, duration: reducedMotion.matches || !animate ? 0 : .28, ease: 'power2.out', overwrite: true });
            gsap.to(trace, { x: x / 3, opacity: role === 'admin' ? .65 : 1,
                duration: reducedMotion.matches || !animate ? 0 : .28, ease: 'power2.out', overwrite: true });
        }
    }

    choices.forEach(choice => {
        choice.addEventListener('click', event => {
            if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) return;
            event.preventDefault();
            if (choice.dataset.role !== page.dataset.loginRole) setRole(choice.dataset.role);
        });
        choice.addEventListener('keydown', event => {
            if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
            event.preventDefault();
            const target = event.key === 'Home' ? choices[0] : event.key === 'End' ? choices[1]
                : choices[(choices.indexOf(choice) + 1) % choices.length];
            target.focus();
            setRole(target.dataset.role);
        });
    });

    function setMode(mode, updateUrl = true, animate = true) {
        const registering = mode === 'register';
        const panel = page.querySelector(registering ? '#register-panel' : '#signin-panel');
        page.dataset.accountMode = mode;
        page.querySelector('#register-panel').hidden = !registering;
        page.querySelector('#signin-panel').hidden = registering;
        page.querySelector('.login-form').setAttribute('aria-labelledby', registering ? 'register-title' : 'login-title');
        if (!registering) alignIndicator();
        if (updateUrl) window.history.replaceState(null, '', registering ? '/register' : choices.find(choice => choice.dataset.role === page.dataset.loginRole).href);
        if (gsap && animate && !reducedMotion.matches) {
            gsap.fromTo(panel, { y: 10, opacity: .4 }, { y: 0, opacity: 1, duration: .25, ease: 'power3.out', clearProps: 'transform,opacity', overwrite: true });
        }
        if (updateUrl) panel.querySelector('h2').focus({ preventScroll: true });
    }
    page.querySelectorAll('a[data-account-mode]').forEach(link => link.addEventListener('click', event => {
        if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) return;
        event.preventDefault();
        setMode(link.dataset.accountMode);
    }));
    const registrationPassword = page.querySelector('#register-password');
    const registrationConfirm = page.querySelector('#register-confirm');
    function checkConfirmation() {
        registrationConfirm.setCustomValidity(registrationConfirm.value && registrationConfirm.value !== registrationPassword.value ? 'Passwords do not match.' : '');
    }
    registrationPassword.addEventListener('input', checkConfirmation);
    registrationConfirm.addEventListener('input', checkConfirmation);
    setRole(initialRole, false, false);
    setMode(page.dataset.accountMode, false, false);
    new ResizeObserver(() => alignIndicator()).observe(tabs);
    document.fonts.ready.then(() => alignIndicator());
    window.addEventListener('popstate', () => {
        setRole(window.location.pathname.startsWith('/admin/') ? 'admin' : 'employee', false);
        setMode(window.location.pathname === '/register' ? 'register' : 'login', false);
    });
    reducedMotion.addEventListener('change', () => {
        setRole(page.dataset.loginRole, false, false);
        if (gsap && reducedMotion.matches) {
            const panels = page.querySelectorAll('#signin-panel, #register-panel, .login-intro, .login-form');
            gsap.killTweensOf(panels);
            gsap.set(panels, { clearProps: 'transform,opacity' });
            gsap.killTweensOf(ambient);
            gsap.set(ambient, { y: 0 });
        }
    });

    if (gsap) {
        const media = gsap.matchMedia();
        let arrived = false;
        media.add('(prefers-reduced-motion: no-preference)', () => {
            if (arrived) return;
            arrived = true;
            gsap.fromTo('.login-intro, .login-form', { y: 8 }, { y: 0, duration: .28, ease: 'power3.out', clearProps: 'transform' });
        });
        if (window.matchMedia('(hover: hover) and (pointer: fine)').matches) {
            const moveLight = gsap.quickTo(ambient, 'y', { duration: .45, ease: 'power3.out' });
            page.addEventListener('pointermove', event => {
                if (!reducedMotion.matches) moveLight((event.clientY / window.innerHeight - .5) * 28);
            });
            page.addEventListener('pointerleave', () => moveLight(0));
        }
    }
})();
