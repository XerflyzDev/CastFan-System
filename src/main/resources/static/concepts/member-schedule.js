const toast = document.querySelector(".concept-toast");
const dateButtons = [...document.querySelectorAll("[data-date]")];
const modeButtons = [...document.querySelectorAll("[data-schedule-mode]")];
let toastTimer;

function showMessage(message) {
    toast.textContent = message;
    toast.classList.add("visible");
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => toast.classList.remove("visible"), 1800);
}

document.querySelectorAll("[data-message]").forEach((control) => {
    control.addEventListener("click", () => showMessage(control.dataset.message));
});

dateButtons.forEach((button) => {
    button.addEventListener("click", () => {
        dateButtons.forEach((dateButton) => {
            const isActive = dateButton === button;
            dateButton.classList.toggle("active", isActive);
            dateButton.toggleAttribute("aria-pressed", isActive);
        });
        showMessage(`Showing schedule for ${button.dataset.date} October`);
    });
});

modeButtons.forEach((button) => {
    button.addEventListener("click", () => {
        modeButtons.forEach((modeButton) => {
            const isActive = modeButton === button;
            modeButton.classList.toggle("active", isActive);
            modeButton.setAttribute("aria-pressed", isActive);
        });
        showMessage(`${button.textContent} view selected`);
    });
});
