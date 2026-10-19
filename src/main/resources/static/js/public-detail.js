const toast = document.querySelector("#app-toast");
let toastTimer;

function showMessage(message) {
    if (!toast || !message) return;
    toast.textContent = message;
    toast.classList.add("visible");
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => toast.classList.remove("visible"), 1800);
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
