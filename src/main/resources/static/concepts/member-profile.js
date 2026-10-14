const toast = document.querySelector(".concept-toast");
const notificationToggle = document.querySelector("[data-notification-toggle]");
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

notificationToggle.addEventListener("change", () => {
    showMessage(notificationToggle.checked ? "Notifications enabled" : "Notifications paused");
});
