const toast = document.querySelector(".concept-toast");
const calendarButton = document.querySelector("[data-calendar-action]");
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

calendarButton.addEventListener("click", () => {
    const isAdded = calendarButton.classList.toggle("added");
    calendarButton.querySelector("i").className = isAdded ? "bi bi-calendar-check" : "bi bi-calendar-plus";
    calendarButton.querySelector("span").textContent = isAdded ? "Added to Calendar" : "Add to Calendar";
    showMessage(isAdded ? "Event added to calendar" : "Event removed from calendar");
});
