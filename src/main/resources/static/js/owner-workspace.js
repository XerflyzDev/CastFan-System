document.querySelectorAll("[data-reject]").forEach((button) => {
    button.addEventListener("click", (event) => {
        const note = button.closest("form")?.querySelector("input[name='reviewNote']");
        if (note && !note.value.trim()) {
            event.preventDefault();
            note.setCustomValidity("Enter a reason before rejecting this shift.");
            note.reportValidity();
            note.addEventListener("input", () => note.setCustomValidity(""), { once: true });
        }
    });
});

document.querySelector("[data-event-image-input]")?.addEventListener("change", (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const preview = document.querySelector("[data-event-image-preview]");
    const fileName = document.querySelector("[data-event-image-name]");
    const removeImage = document.querySelector("[data-event-image-remove]");
    const clearImage = document.querySelector("[data-event-image-clear]");
    if (fileName) fileName.textContent = file.name;
    if (removeImage) removeImage.value = "false";
    if (clearImage) clearImage.hidden = false;
    if (!preview) return;
    const reader = new FileReader();
    reader.addEventListener("load", () => {
        preview.src = reader.result;
        preview.hidden = false;
    }, { once: true });
    reader.readAsDataURL(file);
});

document.querySelector("[data-event-image-clear]")?.addEventListener("click", (event) => {
    const imageInput = document.querySelector("[data-event-image-input]");
    const preview = document.querySelector("[data-event-image-preview]");
    const fileName = document.querySelector("[data-event-image-name]");
    const removeImage = document.querySelector("[data-event-image-remove]");
    if (imageInput) imageInput.value = "";
    if (preview) {
        preview.removeAttribute("src");
        preview.hidden = true;
    }
    if (fileName) fileName.textContent = "Choose image";
    if (removeImage) removeImage.value = "true";
    event.currentTarget.hidden = true;
});

const datePicker = document.querySelector("[data-date-picker]");

if (datePicker) {
    const valueInput = datePicker.querySelector("[data-date-value]");
    const trigger = datePicker.querySelector("[data-date-trigger]");
    const display = datePicker.querySelector("[data-date-display]");
    const popover = datePicker.querySelector("[data-date-popover]");
    const monthLabel = datePicker.querySelector("[data-calendar-month]");
    const daysGrid = datePicker.querySelector("[data-calendar-days]");
    datePicker.classList.add("enhanced");
    valueInput.tabIndex = -1;
    valueInput.setAttribute("aria-hidden", "true");
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    function parseDate(value) {
        const parts = value?.split("-").map(Number);
        if (!parts || parts.length !== 3 || parts.some(Number.isNaN)) return null;
        return new Date(parts[0], parts[1] - 1, parts[2]);
    }

    function toInputValue(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        return `${year}-${month}-${day}`;
    }

    function sameDate(first, second) {
        return first?.getFullYear() === second?.getFullYear()
            && first?.getMonth() === second?.getMonth()
            && first?.getDate() === second?.getDate();
    }

    let selectedDate = parseDate(valueInput.value);
    let visibleMonth = selectedDate
        ? new Date(selectedDate.getFullYear(), selectedDate.getMonth(), 1)
        : new Date(today.getFullYear(), today.getMonth(), 1);

    function updateDisplay() {
        display.textContent = selectedDate
            ? new Intl.DateTimeFormat("en-GB", { day: "numeric", month: "short", year: "numeric" }).format(selectedDate)
            : "Choose date";
    }

    function selectDate(date) {
        selectedDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
        visibleMonth = new Date(date.getFullYear(), date.getMonth(), 1);
        valueInput.value = toInputValue(selectedDate);
        trigger.removeAttribute("aria-invalid");
        valueInput.dispatchEvent(new Event("change", { bubbles: true }));
        updateDisplay();
        renderCalendar();
    }

    function renderCalendar() {
        monthLabel.textContent = new Intl.DateTimeFormat("en-GB", { month: "long", year: "numeric" }).format(visibleMonth);
        daysGrid.replaceChildren();
        const gridStart = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth(), 1 - visibleMonth.getDay());

        for (let index = 0; index < 42; index += 1) {
            const date = new Date(gridStart.getFullYear(), gridStart.getMonth(), gridStart.getDate() + index);
            const button = document.createElement("button");
            button.type = "button";
            button.className = "calendar-day";
            button.textContent = String(date.getDate());
            button.setAttribute("role", "gridcell");
            button.setAttribute("aria-label", new Intl.DateTimeFormat("en-GB", { dateStyle: "full" }).format(date));
            if (date.getMonth() !== visibleMonth.getMonth()) button.classList.add("outside");
            if (sameDate(date, today)) button.classList.add("today");
            if (sameDate(date, selectedDate)) {
                button.classList.add("selected");
                button.setAttribute("aria-selected", "true");
            }
            button.addEventListener("click", () => {
                selectDate(date);
                closeCalendar();
                trigger.focus();
            });
            daysGrid.append(button);
        }
    }

    function openCalendar() {
        document.querySelectorAll("[data-time-popover]").forEach((timePopover) => {
            timePopover.hidden = true;
            timePopover.closest("[data-time-picker]")?.querySelector("[data-time-trigger]")?.setAttribute("aria-expanded", "false");
        });
        popover.hidden = false;
        trigger.setAttribute("aria-expanded", "true");
        renderCalendar();
        (popover.querySelector(".selected")
            || popover.querySelector(".today")
            || popover.querySelector(".calendar-day"))?.focus();
    }

    function closeCalendar() {
        popover.hidden = true;
        trigger.setAttribute("aria-expanded", "false");
    }

    trigger.addEventListener("click", () => popover.hidden ? openCalendar() : closeCalendar());
    valueInput.addEventListener("invalid", (event) => {
        event.preventDefault();
        trigger.setAttribute("aria-invalid", "true");
        openCalendar();
        trigger.focus();
    });
    datePicker.querySelector("[data-calendar-previous]").addEventListener("click", () => {
        visibleMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() - 1, 1);
        renderCalendar();
    });
    datePicker.querySelector("[data-calendar-next]").addEventListener("click", () => {
        visibleMonth = new Date(visibleMonth.getFullYear(), visibleMonth.getMonth() + 1, 1);
        renderCalendar();
    });
    datePicker.querySelector("[data-calendar-today]").addEventListener("click", () => {
        selectDate(today);
        closeCalendar();
        trigger.focus();
    });
    datePicker.querySelector("[data-calendar-clear]").addEventListener("click", () => {
        selectedDate = null;
        valueInput.value = "";
        trigger.setAttribute("aria-invalid", "true");
        valueInput.dispatchEvent(new Event("change", { bubbles: true }));
        updateDisplay();
        renderCalendar();
        closeCalendar();
        trigger.focus();
    });
    document.addEventListener("click", (event) => {
        if (!popover.hidden && !datePicker.contains(event.target)) closeCalendar();
    });
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !popover.hidden) {
            closeCalendar();
            trigger.focus();
        }
    });

    updateDisplay();
}

document.querySelectorAll("[data-time-picker]").forEach((timePicker) => {
    const valueInput = timePicker.querySelector("[data-time-value]");
    const trigger = timePicker.querySelector("[data-time-trigger]");
    const display = timePicker.querySelector("[data-time-display]");
    const popover = timePicker.querySelector("[data-time-popover]");
    const hoursList = timePicker.querySelector("[data-time-hours]");
    const minutesList = timePicker.querySelector("[data-time-minutes]");
    let selectedHour = null;
    let selectedMinute = null;

    timePicker.classList.add("enhanced");
    valueInput.tabIndex = -1;
    valueInput.setAttribute("aria-hidden", "true");

    function readValue() {
        const parts = valueInput.value?.split(":").map(Number);
        if (!parts || parts.length < 2 || parts.some(Number.isNaN)) return;
        selectedHour = parts[0];
        selectedMinute = parts[1];
    }

    function updateTimeValue() {
        if (selectedHour === null || selectedMinute === null) {
            valueInput.value = "";
            display.textContent = "Choose time";
            return;
        }
        valueInput.value = `${String(selectedHour).padStart(2, "0")}:${String(selectedMinute).padStart(2, "0")}`;
        display.textContent = valueInput.value;
        trigger.removeAttribute("aria-invalid");
        valueInput.dispatchEvent(new Event("change", { bubbles: true }));
    }

    function buildOptions(container, total, selectedValue, onSelect) {
        container.replaceChildren();
        for (let value = 0; value < total; value += 1) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "time-option";
            button.textContent = String(value).padStart(2, "0");
            button.setAttribute("role", "option");
            if (value === selectedValue) {
                button.classList.add("selected");
                button.setAttribute("aria-selected", "true");
            }
            button.addEventListener("click", () => {
                onSelect(value);
                renderOptions();
                updateTimeValue();
            });
            container.append(button);
        }
    }

    function renderOptions() {
        buildOptions(hoursList, 24, selectedHour, (value) => { selectedHour = value; });
        buildOptions(minutesList, 60, selectedMinute, (value) => { selectedMinute = value; });
        requestAnimationFrame(() => {
            [hoursList, minutesList].forEach((list) => {
                const selected = list.querySelector(".selected");
                if (selected) list.scrollTop = selected.offsetTop - ((list.clientHeight - selected.offsetHeight) / 2);
            });
        });
    }

    function openTimePicker() {
        document.querySelectorAll("[data-time-popover]").forEach((other) => {
            if (other !== popover) {
                other.hidden = true;
                other.closest("[data-time-picker]")?.querySelector("[data-time-trigger]")?.setAttribute("aria-expanded", "false");
            }
        });
        const calendarPopover = document.querySelector("[data-date-popover]");
        if (calendarPopover) calendarPopover.hidden = true;
        document.querySelector("[data-date-trigger]")?.setAttribute("aria-expanded", "false");
        if (selectedHour === null || selectedMinute === null) {
            const now = new Date();
            selectedHour = now.getHours();
            selectedMinute = now.getMinutes();
        }
        popover.hidden = false;
        trigger.setAttribute("aria-expanded", "true");
        renderOptions();
        popover.querySelector(".selected")?.focus();
    }

    function closeTimePicker() {
        popover.hidden = true;
        trigger.setAttribute("aria-expanded", "false");
    }

    trigger.addEventListener("click", () => popover.hidden ? openTimePicker() : closeTimePicker());
    valueInput.addEventListener("invalid", (event) => {
        event.preventDefault();
        trigger.setAttribute("aria-invalid", "true");
        openTimePicker();
        trigger.focus();
    });
    timePicker.querySelector("[data-time-close]").addEventListener("click", () => {
        closeTimePicker();
        trigger.focus();
    });
    timePicker.querySelector("[data-time-done]").addEventListener("click", () => {
        updateTimeValue();
        closeTimePicker();
        trigger.focus();
    });
    timePicker.querySelector("[data-time-now]").addEventListener("click", () => {
        const now = new Date();
        selectedHour = now.getHours();
        selectedMinute = now.getMinutes();
        updateTimeValue();
        renderOptions();
    });
    timePicker.querySelector("[data-time-clear]").addEventListener("click", () => {
        selectedHour = null;
        selectedMinute = null;
        trigger.setAttribute("aria-invalid", "true");
        updateTimeValue();
        closeTimePicker();
        trigger.focus();
    });
    document.addEventListener("click", (event) => {
        if (!popover.hidden && !timePicker.contains(event.target)) closeTimePicker();
    });
    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && !popover.hidden) {
            closeTimePicker();
            trigger.focus();
        }
    });

    readValue();
    updateTimeValue();
});
