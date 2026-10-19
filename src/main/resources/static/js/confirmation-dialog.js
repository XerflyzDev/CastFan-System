const confirmationForms = document.querySelectorAll("form[data-confirm]");

if (confirmationForms.length > 0) {
    const dialog = document.createElement("dialog");
    dialog.className = "confirmation-dialog";
    dialog.setAttribute("aria-labelledby", "confirmation-dialog-title");
    dialog.setAttribute("aria-describedby", "confirmation-dialog-message");
    dialog.innerHTML = `
        <div class="confirmation-panel">
            <span class="confirmation-icon" aria-hidden="true"><i class="bi bi-exclamation-lg"></i></span>
            <div class="confirmation-copy">
                <small>Please confirm</small>
                <h2 id="confirmation-dialog-title">Confirm action</h2>
                <p id="confirmation-dialog-message"></p>
            </div>
            <div class="confirmation-actions">
                <button class="confirmation-cancel" type="button">Keep it</button>
                <button class="confirmation-submit" type="button">Confirm</button>
            </div>
        </div>`;
    document.body.append(dialog);

    const title = dialog.querySelector("#confirmation-dialog-title");
    const message = dialog.querySelector("#confirmation-dialog-message");
    const cancel = dialog.querySelector(".confirmation-cancel");
    const confirm = dialog.querySelector(".confirmation-submit");
    let pendingSubmission = null;

    function actionLabel(prompt) {
        const firstWord = prompt.trim().split(/\s+/)[0]?.replace(/[^a-z]/gi, "");
        return firstWord || "Confirm";
    }

    confirmationForms.forEach((form) => {
        form.addEventListener("submit", (event) => {
            if (form.dataset.confirmApproved === "true") {
                delete form.dataset.confirmApproved;
                return;
            }
            event.preventDefault();
            const action = actionLabel(form.dataset.confirm || "Confirm this action?");
            pendingSubmission = { form, submitter: event.submitter };
            title.textContent = `${action} this item?`;
            message.textContent = form.dataset.confirm || "Please confirm this action.";
            confirm.textContent = action;
            dialog.showModal();
            cancel.focus();
        });
    });

    cancel.addEventListener("click", () => dialog.close("cancel"));
    confirm.addEventListener("click", () => {
        if (!pendingSubmission) return dialog.close("cancel");
        const { form, submitter } = pendingSubmission;
        form.dataset.confirmApproved = "true";
        dialog.close("confirm");
        form.requestSubmit(submitter || undefined);
    });
    dialog.addEventListener("close", () => {
        pendingSubmission = null;
    });
    dialog.addEventListener("click", (event) => {
        if (event.target === dialog) dialog.close("cancel");
    });
}
