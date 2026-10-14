const toast = document.querySelector("#app-toast");
const localTime = document.querySelector("#local-time");
let toastTimer;

function showMessage(message) {
    if (!toast || !message) return;
    toast.textContent = message;
    toast.classList.add("visible");
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => toast.classList.remove("visible"), 1800);
}

function updateLocalTime() {
    if (!localTime) return;
    localTime.textContent = new Intl.DateTimeFormat("en-GB", {
        timeZone: "Asia/Bangkok",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    }).format(new Date());
}

document.querySelectorAll("[data-message]").forEach((control) => {
    control.addEventListener("click", () => showMessage(control.dataset.message));
});

updateLocalTime();
window.setInterval(updateLocalTime, 30_000);
