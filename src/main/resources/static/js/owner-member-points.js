document.querySelectorAll('[data-confirm-points]').forEach((form) => {
    form.addEventListener('submit', () => {
        const button = form.querySelector('button[type="submit"]');
        if (!button) return;
        button.disabled = true;
        button.setAttribute('aria-busy', 'true');
        const label = button.querySelector('span');
        if (label) label.textContent = 'Recording points…';
    });
});

document.querySelectorAll('[data-reversal-form]').forEach((form) => {
    form.addEventListener('submit', (event) => {
        const reason = form.querySelector('[name="reason"]')?.value.trim();
        if (!reason || !window.confirm('Reverse these purchase points? The balance and tier will update immediately.')) {
            event.preventDefault();
        }
    });
});
