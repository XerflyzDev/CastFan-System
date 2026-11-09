document.querySelectorAll("[data-shift-review-form]").forEach((form) => {
    form.addEventListener("submit", (event) => {
        if (!form.reportValidity()) {
            event.preventDefault();
            return;
        }
        const card = form.closest("[data-shift-id]");
        if (!card || card.dataset.submitting === "true") {
            event.preventDefault();
            return;
        }
        card.dataset.submitting = "true";
        card.classList.add("is-submitting");
        card.setAttribute("aria-busy", "true");
        const action = form.dataset.reviewAction;
        const status = card.querySelector("[data-submit-status]");
        if (status) status.textContent = action === "approve" ? "Submitting approval…" : "Submitting rejection…";
        card.querySelectorAll("button[type='submit']").forEach((button) => {
            button.disabled = true;
            if (button.form === form) {
                button.classList.add("is-loading");
                button.textContent = action === "approve" ? "Approving…" : "Rejecting…";
            }
        });
    });
});
