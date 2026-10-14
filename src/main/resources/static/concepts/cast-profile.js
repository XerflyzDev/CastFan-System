const toast = document.querySelector(".concept-toast");
const followButton = document.querySelector("[data-profile-follow]");
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

followButton.addEventListener("click", () => {
    const isFollowing = followButton.classList.toggle("active");
    followButton.setAttribute("aria-pressed", isFollowing);
    followButton.querySelector("i").className = isFollowing ? "bi bi-heart-fill" : "bi bi-heart";
    followButton.querySelector("span").textContent = isFollowing ? "Following" : "Follow";
    showMessage(isFollowing ? "Mina followed" : "Mina unfollowed");
});
