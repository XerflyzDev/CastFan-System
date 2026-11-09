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

document.querySelectorAll("[data-owner-date-form]").forEach((form) => {
    const input = form.querySelector("[data-owner-date-input]");
    const trigger = form.querySelector("[data-owner-date-trigger]");
    const navigator = form.closest(".owner-schedule-navigator");
    const panel = navigator?.querySelector("[data-owner-calendar-panel]");
    const grid = panel?.querySelector("[data-owner-calendar-grid]");
    const monthLabel = panel?.querySelector("[data-owner-calendar-month]");
    const previous = panel?.querySelector("[data-owner-calendar-previous]");
    const next = panel?.querySelector("[data-owner-calendar-next]");
    const todayButton = panel?.querySelector("[data-owner-calendar-today]");
    if (!input || !trigger || !panel || !grid || !monthLabel || !previous || !next || !todayButton) return;

    const parseDate = (value) => {
        const [year, month, day] = value.split("-").map(Number);
        return new Date(year, month - 1, day);
    };
    const toIsoDate = (date) => [
        date.getFullYear(),
        String(date.getMonth() + 1).padStart(2, "0"),
        String(date.getDate()).padStart(2, "0")
    ].join("-");
    const selectedDate = parseDate(input.value);
    const today = parseDate(input.dataset.today);
    let visibleMonth = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), 1);

    const chooseDate = (date) => {
        input.value = toIsoDate(date);
        form.requestSubmit();
    };
    const renderCalendar = () => {
        monthLabel.textContent = visibleMonth.toLocaleDateString("en-US", { month: "long", year: "numeric" });
        grid.replaceChildren();
        const firstVisibleDate = new Date(visibleMonth);
        firstVisibleDate.setDate(1 - visibleMonth.getDay());
        for (let offset = 0; offset < 42; offset += 1) {
            const date = new Date(firstVisibleDate);
            date.setDate(firstVisibleDate.getDate() + offset);
            const button = document.createElement("button");
            button.type = "button";
            button.textContent = String(date.getDate());
            button.setAttribute("aria-label", date.toLocaleDateString("en-US", {
                weekday: "long", day: "numeric", month: "long", year: "numeric"
            }));
            if (date.getMonth() !== visibleMonth.getMonth()) button.classList.add("is-outside-month");
            if (toIsoDate(date) === input.value) {
                button.classList.add("is-selected");
                button.setAttribute("aria-current", "date");
            }
            if (toIsoDate(date) === toIsoDate(today)) button.classList.add("is-today");
            button.addEventListener("click", () => chooseDate(date));
            grid.append(button);
        }
    };
    const setOpen = (open) => {
        panel.hidden = !open;
        trigger.setAttribute("aria-expanded", String(open));
        if (open) renderCalendar();
    };

    trigger.addEventListener("click", () => {
        setOpen(panel.hidden);
    });
    previous.addEventListener("click", () => {
        visibleMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() - 1, 1);
        renderCalendar();
    });
    next.addEventListener("click", () => {
        visibleMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() + 1, 1);
        renderCalendar();
    });
    todayButton.addEventListener("click", () => chooseDate(today));
    document.addEventListener("click", (event) => {
        if (!panel.hidden && !navigator.contains(event.target)) setOpen(false);
    });
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !panel.hidden) {
            setOpen(false);
            trigger.focus();
        }
    });
});
