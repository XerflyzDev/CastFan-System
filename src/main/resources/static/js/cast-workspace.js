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

document.querySelectorAll("form[data-confirm]").forEach((form) => {
    form.addEventListener("submit", (event) => {
        if (!window.confirm(form.dataset.confirm)) event.preventDefault();
    });
});

const audience = document.querySelector("[data-content-audience]");
const tierField = document.querySelector("[data-tier-field]");

function updateTierField() {
    if (!audience || !tierField) return;
    const membersOnly = audience.value === "MEMBERS";
    tierField.hidden = !membersOnly;
    const select = tierField.querySelector("select");
    if (select) select.required = membersOnly;
}

audience?.addEventListener("change", updateTierField);
updateTierField();

updateLocalTime();
window.setInterval(updateLocalTime, 30_000);
