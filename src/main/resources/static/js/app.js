const searchInput = document.querySelector("#cast-search");
const clearSearchButton = document.querySelector(".search-clear");
const toast = document.querySelector("#app-toast");
let toastTimer;

function showToast(message) {
    if (!toast || !message) {
        return;
    }

    window.clearTimeout(toastTimer);
    toast.textContent = message;
    toast.classList.add("visible");
    toastTimer = window.setTimeout(() => toast.classList.remove("visible"), 2200);
}

function filterCastList(listId, emptyId, query) {
    const list = document.querySelector(`#${listId}`);
    const emptyState = document.querySelector(`#${emptyId}`);
    if (!list || !emptyState) {
        return;
    }

    const items = [...list.querySelectorAll("[data-search-item]")];
    let visibleCount = 0;

    items.forEach((item) => {
        const searchableText = item.dataset.searchItem.toLocaleLowerCase("th");
        const isVisible = searchableText.includes(query);
        item.hidden = !isVisible;
        visibleCount += isVisible ? 1 : 0;
    });

    emptyState.hidden = visibleCount > 0;
}

function applySearch() {
    if (!searchInput) {
        return;
    }

    const query = searchInput.value.trim().toLocaleLowerCase("th");
    clearSearchButton.hidden = query.length === 0;
    filterCastList("working-cast-list", "working-empty", query);
    filterCastList("following-cast-list", "following-empty", query);
}

searchInput?.addEventListener("input", applySearch);

clearSearchButton?.addEventListener("click", () => {
    searchInput.value = "";
    searchInput.focus();
    applySearch();
});

document.addEventListener("click", (event) => {
    const toastTrigger = event.target.closest("[data-toast]");
    if (toastTrigger) {
        showToast(toastTrigger.dataset.toast);
    }

    const followButton = event.target.closest(".follow-button");
    if (!followButton) {
        return;
    }

    const isFollowing = followButton.classList.toggle("is-following");
    followButton.setAttribute("aria-pressed", String(isFollowing));
    followButton.querySelector("i").className = isFollowing ? "bi bi-check-lg" : "bi bi-plus-lg";
    followButton.querySelector("span").textContent = isFollowing ? "Following" : "Follow";
    showToast(`${isFollowing ? "Now following" : "Unfollowed"} ${followButton.dataset.castName}`);
});
