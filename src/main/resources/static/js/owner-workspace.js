const localTime = document.querySelector("#local-time");

function updateLocalTime() {
    if (!localTime) return;
    localTime.textContent = new Intl.DateTimeFormat("en-GB", {
        timeZone: "Asia/Bangkok",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    }).format(new Date());
}

document.querySelectorAll("[data-reject]").forEach((button) => {
    button.addEventListener("click", (event) => {
        const note = button.closest("form")?.querySelector("input[name='reviewNote']");
        if (note && !note.value.trim()) {
            event.preventDefault();
            note.setCustomValidity("Enter a reason before rejecting this shift.");
            note.reportValidity();
            note.addEventListener("input", () => note.setCustomValidity(""), { once: true });
        }
    });
});

document.querySelectorAll("[data-confirm]").forEach((form) => {
    form.addEventListener("submit", (event) => {
        if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    });
});

updateLocalTime();
window.setInterval(updateLocalTime, 30_000);
