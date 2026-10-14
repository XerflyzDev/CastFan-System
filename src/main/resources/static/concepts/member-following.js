const toast = document.querySelector(".concept-toast");
const search = document.querySelector("#following-search");
const cards = [...document.querySelectorAll("[data-following-card]")];
const filterButtons = [...document.querySelectorAll("[data-following-filter]")];
const emptyState = document.querySelector("[data-following-empty]");
let activeFilter = "all";
let toastTimer;

function showMessage(message) {
    toast.textContent = message;
    toast.classList.add("visible");
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => toast.classList.remove("visible"), 1800);
}

function filterCards() {
    const query = search.value.trim().toLowerCase();
    let visibleCount = 0;

    cards.forEach((card) => {
        const matchesSearch = card.dataset.castName.toLowerCase().includes(query);
        const matchesFilter = activeFilter === "all"
            || (activeFilter === "working" && card.dataset.working === "true")
            || (activeFilter === "membership" && card.dataset.membership === "true");
        const isVisible = matchesSearch && matchesFilter;
        card.hidden = !isVisible;
        visibleCount += isVisible ? 1 : 0;
    });

    emptyState.hidden = visibleCount > 0;
}

document.querySelectorAll("[data-message]").forEach((control) => {
    control.addEventListener("click", () => showMessage(control.dataset.message));
});

document.querySelector("[data-clear-following-search]").addEventListener("click", () => {
    search.value = "";
    search.focus();
    filterCards();
});

search.addEventListener("input", filterCards);

filterButtons.forEach((button) => {
    button.addEventListener("click", () => {
        activeFilter = button.dataset.followingFilter;
        filterButtons.forEach((filterButton) => {
            const isActive = filterButton === button;
            filterButton.classList.toggle("active", isActive);
            filterButton.setAttribute("aria-pressed", isActive);
        });
        filterCards();
    });
});

document.querySelectorAll("[data-follow-toggle]").forEach((button) => {
    button.classList.add("is-following");
    button.addEventListener("click", () => {
        const isFollowing = button.classList.toggle("is-following");
        button.setAttribute("aria-pressed", isFollowing);
        button.querySelector("i").className = isFollowing ? "bi bi-heart-fill" : "bi bi-heart";
        button.querySelector("span").textContent = isFollowing ? "Following" : "Follow";
        const castName = button.closest("[data-following-card]").dataset.castName;
        button.setAttribute("aria-label", `${isFollowing ? "Unfollow" : "Follow"} ${castName}`);
        showMessage(isFollowing ? "Cast followed" : "Cast unfollowed");
    });
});
