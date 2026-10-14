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

document.querySelector("[data-share-profile]")?.addEventListener("click", async (event) => {
    const title = event.currentTarget.dataset.shareTitle;
    try {
        if (navigator.share) {
            await navigator.share({ title, url: window.location.href });
        } else {
            await navigator.clipboard.writeText(window.location.href);
            showMessage("Link copied");
        }
    } catch (error) {
        if (error.name !== "AbortError") showMessage("Sharing is unavailable");
    }
});

updateLocalTime();
window.setInterval(updateLocalTime, 30_000);
