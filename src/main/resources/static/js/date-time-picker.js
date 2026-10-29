let activePicker = null;

function closeActivePicker(returnFocus = false) {
    if (!activePicker) return;
    activePicker.popover.hidden = true;
    activePicker.trigger.setAttribute("aria-expanded", "false");
    if (returnFocus) activePicker.trigger.focus();
    activePicker = null;
}

function openPicker(host, trigger, popover) {
    closeActivePicker();
    popover.hidden = false;
    const triggerRect = trigger.getBoundingClientRect();
    const boundary = host.closest(".phone") || document.documentElement;
    const boundaryRect = boundary.getBoundingClientRect();
    const margin = 12;
    const minLeft = boundaryRect.left + margin;
    const maxWidth = Math.max(0, boundaryRect.width - margin * 2);
    popover.style.maxWidth = `${maxWidth}px`;
    popover.style.maxHeight = `${Math.max(0, boundaryRect.height - margin * 2)}px`;
    const popoverWidth = popover.getBoundingClientRect().width;
    const popoverHeight = popover.getBoundingClientRect().height;
    const left = Math.min(
        Math.max(minLeft, triggerRect.right - popoverWidth),
        Math.max(minLeft, boundaryRect.right - popoverWidth - margin)
    );
    const minTop = boundaryRect.top + margin;
    const maxTop = Math.max(minTop, boundaryRect.bottom - popoverHeight - margin);
    const spaceAbove = triggerRect.top - minTop;
    const spaceBelow = boundaryRect.bottom - triggerRect.bottom - margin;
    const openDown = spaceAbove < popoverHeight + 7 && spaceBelow > spaceAbove;
    const top = openDown
        ? Math.min(maxTop, triggerRect.bottom + 7)
        : Math.max(minTop, Math.min(maxTop, triggerRect.top - popoverHeight - 7));
    popover.classList.toggle("open-down", openDown);
    popover.style.left = `${left}px`;
    popover.style.top = `${top}px`;
    popover.style.right = "auto";
    popover.style.bottom = "auto";
    trigger.setAttribute("aria-expanded", "true");
    activePicker = { host, trigger, popover };
}

function parseDateValue(value) {
    const parts = value?.split("-").map(Number);
    if (!parts || parts.length !== 3 || parts.some(Number.isNaN)) return null;
    return new Date(parts[0], parts[1] - 1, parts[2]);
}

function dateValue(date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function sameDate(first, second) {
    return first?.getFullYear() === second?.getFullYear()
        && first?.getMonth() === second?.getMonth()
        && first?.getDate() === second?.getDate();
}

document.querySelectorAll("input[data-picker-date]").forEach((input) => {
    const host = input.closest("[data-picker-host]");
    if (!host) return;
    const label = input.dataset.pickerLabel || "Choose date";
    const trigger = document.createElement("button");
    trigger.className = "picker-trigger";
    trigger.type = "button";
    trigger.setAttribute("aria-haspopup", "dialog");
    trigger.setAttribute("aria-expanded", "false");
    trigger.innerHTML = '<span></span><i class="bi bi-calendar3" aria-hidden="true"></i>';
    const display = trigger.querySelector("span");
    const popover = document.createElement("div");
    popover.className = "picker-popover shared-date-popover";
    popover.hidden = true;
    popover.setAttribute("role", "dialog");
    popover.setAttribute("aria-label", label);
    popover.innerHTML = '<header class="picker-popover-header"><strong></strong><span><button class="picker-icon-button" type="button" data-previous aria-label="Previous month" title="Previous month"><i class="bi bi-chevron-left" aria-hidden="true"></i></button><button class="picker-icon-button" type="button" data-next aria-label="Next month" title="Next month"><i class="bi bi-chevron-right" aria-hidden="true"></i></button></span></header><div class="shared-calendar-weekdays" aria-hidden="true"><span>Su</span><span>Mo</span><span>Tu</span><span>We</span><span>Th</span><span>Fr</span><span>Sa</span></div><div class="shared-calendar-days" role="grid"></div><footer class="picker-popover-footer"><button type="button" data-clear>Clear</button><button type="button" data-today>Today</button></footer>';
    host.append(trigger, popover);
    host.classList.add("picker-host", "enhanced");
    input.tabIndex = -1;
    input.setAttribute("aria-hidden", "true");

    const monthLabel = popover.querySelector("header strong");
    const days = popover.querySelector(".shared-calendar-days");
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    let selected = parseDateValue(input.value);
    let visibleMonth = selected ? new Date(selected.getFullYear(), selected.getMonth(), 1) : new Date(today.getFullYear(), today.getMonth(), 1);

    function updateDisplay() {
        display.textContent = selected
            ? new Intl.DateTimeFormat("en-GB", { day: "numeric", month: "short", year: "numeric" }).format(selected)
            : label;
    }

    function selectDate(date) {
        selected = new Date(date.getFullYear(), date.getMonth(), date.getDate());
        visibleMonth = new Date(date.getFullYear(), date.getMonth(), 1);
        input.value = dateValue(selected);
        trigger.removeAttribute("aria-invalid");
        input.dispatchEvent(new Event("change", { bubbles: true }));
        updateDisplay();
        render();
    }

    function render() {
        monthLabel.textContent = new Intl.DateTimeFormat("en-GB", { month: "long", year: "numeric" }).format(visibleMonth);
        days.replaceChildren();
        const first = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth(), 1 - visibleMonth.getDay());
        for (let index = 0; index < 42; index += 1) {
            const date = new Date(first.getFullYear(), first.getMonth(), first.getDate() + index);
            const button = document.createElement("button");
            button.type = "button";
            button.className = "shared-calendar-day";
            button.textContent = String(date.getDate());
            button.setAttribute("role", "gridcell");
            button.setAttribute("aria-label", new Intl.DateTimeFormat("en-GB", { dateStyle: "full" }).format(date));
            if (date.getMonth() !== visibleMonth.getMonth()) button.classList.add("outside");
            if (sameDate(date, today)) button.classList.add("today");
            if (sameDate(date, selected)) {
                button.classList.add("selected");
                button.setAttribute("aria-selected", "true");
            }
            button.addEventListener("click", () => {
                selectDate(date);
                closeActivePicker(true);
            });
            days.append(button);
        }
    }

    trigger.addEventListener("click", () => {
        if (activePicker?.popover === popover) return closeActivePicker();
        openPicker(host, trigger, popover);
        render();
        (popover.querySelector(".selected") || popover.querySelector(".today") || popover.querySelector(".shared-calendar-day"))?.focus();
    });
    input.addEventListener("invalid", (event) => {
        event.preventDefault();
        trigger.setAttribute("aria-invalid", "true");
        openPicker(host, trigger, popover);
        render();
        trigger.focus();
    });
    popover.querySelector("[data-previous]").addEventListener("click", () => {
        visibleMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() - 1, 1);
        render();
    });
    popover.querySelector("[data-next]").addEventListener("click", () => {
        visibleMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() + 1, 1);
        render();
    });
    popover.querySelector("[data-today]").addEventListener("click", () => {
        selectDate(today);
        closeActivePicker(true);
    });
    popover.querySelector("[data-clear]").addEventListener("click", () => {
        selected = null;
        input.value = "";
        trigger.setAttribute("aria-invalid", "true");
        input.dispatchEvent(new Event("change", { bubbles: true }));
        updateDisplay();
        closeActivePicker(true);
    });
    updateDisplay();
});

document.querySelectorAll("input[data-picker-time]").forEach((input) => {
    const host = input.closest("[data-picker-host]");
    if (!host) return;
    const label = input.dataset.pickerLabel || "Choose time";
    const trigger = document.createElement("button");
    trigger.className = "picker-trigger";
    trigger.type = "button";
    trigger.setAttribute("aria-haspopup", "dialog");
    trigger.setAttribute("aria-expanded", "false");
    trigger.innerHTML = '<span></span><i class="bi bi-clock" aria-hidden="true"></i>';
    const display = trigger.querySelector("span");
    const popover = document.createElement("div");
    popover.className = "picker-popover shared-time-popover";
    popover.hidden = true;
    popover.setAttribute("role", "dialog");
    popover.setAttribute("aria-label", label);
    popover.innerHTML = '<header class="picker-popover-header"><strong></strong><button class="picker-icon-button" type="button" data-close aria-label="Close time picker" title="Close"><i class="bi bi-x-lg" aria-hidden="true"></i></button></header><div class="shared-time-columns"><div><small>Hour</small><div class="shared-time-options" role="listbox" aria-label="Hour" data-hours></div></div><span aria-hidden="true">:</span><div><small>Minute</small><div class="shared-time-options" role="listbox" aria-label="Minute" data-minutes></div></div></div><footer class="picker-popover-footer"><button type="button" data-clear>Clear</button><button type="button" data-now>Now</button><button class="picker-done" type="button" data-done>Done</button></footer>';
    popover.querySelector("header strong").textContent = label;
    host.append(trigger, popover);
    host.classList.add("picker-host", "enhanced");
    input.tabIndex = -1;
    input.setAttribute("aria-hidden", "true");

    const hours = popover.querySelector("[data-hours]");
    const minutes = popover.querySelector("[data-minutes]");
    let selectedHour = null;
    let selectedMinute = null;
    const parts = input.value?.split(":").map(Number);
    if (parts?.length >= 2 && !parts.some(Number.isNaN)) [selectedHour, selectedMinute] = parts;

    function updateValue() {
        if (selectedHour === null || selectedMinute === null) {
            input.value = "";
            display.textContent = label;
            return;
        }
        input.value = `${String(selectedHour).padStart(2, "0")}:${String(selectedMinute).padStart(2, "0")}`;
        display.textContent = input.value;
        trigger.removeAttribute("aria-invalid");
        input.dispatchEvent(new Event("change", { bubbles: true }));
    }

    function buildOptions(container, total, selectedValue, select) {
        container.replaceChildren();
        for (let value = 0; value < total; value += 1) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "shared-time-option";
            button.textContent = String(value).padStart(2, "0");
            button.setAttribute("role", "option");
            if (value === selectedValue) {
                button.classList.add("selected");
                button.setAttribute("aria-selected", "true");
            }
            button.addEventListener("click", () => {
                select(value);
                render(true);
                updateValue();
            });
            container.append(button);
        }
    }

    function render(animateSelection = false) {
        buildOptions(hours, 24, selectedHour, (value) => { selectedHour = value; });
        buildOptions(minutes, 60, selectedMinute, (value) => { selectedMinute = value; });
        requestAnimationFrame(() => {
            [hours, minutes].forEach((list) => {
                const selectedOption = list.querySelector(".selected");
                if (!selectedOption) return;
                const listRect = list.getBoundingClientRect();
                const optionRect = selectedOption.getBoundingClientRect();
                const offsetFromCenter = optionRect.top - listRect.top
                    - ((list.clientHeight - optionRect.height) / 2);
                if (animateSelection && !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
                    list.scrollTo({ top: list.scrollTop + offsetFromCenter, behavior: "smooth" });
                    return;
                }
                const previousScrollBehavior = list.style.scrollBehavior;
                list.style.scrollBehavior = "auto";
                list.scrollTop += offsetFromCenter;
                list.style.scrollBehavior = previousScrollBehavior;
            });
        });
    }

    function prepare() {
        if (selectedHour !== null && selectedMinute !== null) return;
        const now = new Date();
        selectedHour = now.getHours();
        selectedMinute = now.getMinutes();
    }

    trigger.addEventListener("click", () => {
        if (activePicker?.popover === popover) return closeActivePicker();
        prepare();
        openPicker(host, trigger, popover);
        render();
        popover.querySelector(".selected")?.focus();
    });
    input.addEventListener("invalid", (event) => {
        event.preventDefault();
        trigger.setAttribute("aria-invalid", "true");
        prepare();
        openPicker(host, trigger, popover);
        render();
        trigger.focus();
    });
    popover.querySelector("[data-close]").addEventListener("click", () => closeActivePicker(true));
    popover.querySelector("[data-done]").addEventListener("click", () => {
        updateValue();
        closeActivePicker(true);
    });
    popover.querySelector("[data-now]").addEventListener("click", () => {
        const now = new Date();
        selectedHour = now.getHours();
        selectedMinute = now.getMinutes();
        updateValue();
        render(true);
    });
    popover.querySelector("[data-clear]").addEventListener("click", () => {
        selectedHour = null;
        selectedMinute = null;
        trigger.setAttribute("aria-invalid", "true");
        updateValue();
        render();
    });
    updateValue();
});

document.addEventListener("click", (event) => {
    // Selecting an option rebuilds the option buttons. Its original event target
    // may therefore be detached by the time this document handler runs. Check
    // the event's original propagation path so that selection does not look like
    // an outside click and dismiss the picker on touch browsers.
    if (activePicker && !event.composedPath().includes(activePicker.host)) closeActivePicker();
});

document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && activePicker) closeActivePicker(true);
});
