const toast = document.querySelector("#app-toast");
const search = document.querySelector("#following-search");
const cards = [...document.querySelectorAll("[data-following-card]")];
const emptyState = document.querySelector("[data-following-empty]");
let toastTimer;

function showMessage(message) {
    if (!toast || !message) return;
    toast.textContent = message;
    toast.classList.add("visible");
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => toast.classList.remove("visible"), 1800);
}

function filterCards() {
    if (!search) return;
    const query = search.value.trim().toLowerCase();
    let visibleCount = 0;
    cards.forEach((card) => {
        const visible = card.dataset.castName.toLowerCase().includes(query);
        card.hidden = !visible;
        visibleCount += visible ? 1 : 0;
    });
    if (emptyState) emptyState.hidden = visibleCount > 0;
}

document.querySelectorAll("[data-message]").forEach((control) => {
    control.addEventListener("click", () => showMessage(control.dataset.message));
});

document.querySelector("[data-clear-following-search]")?.addEventListener("click", () => {
    search.value = "";
    search.focus();
    filterCards();
});

search?.addEventListener("input", filterCards);
