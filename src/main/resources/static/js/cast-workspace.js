const audienceFields = document.querySelectorAll("[data-content-audience]");
const tierField = document.querySelector("[data-tier-field]");

function updateTierField() {
	if (!audienceFields.length || !tierField) return;
	const audience = [...audienceFields].find((field) => field.checked) ?? audienceFields[0];
	const membersOnly = audience.value === "MEMBERS";
    tierField.hidden = !membersOnly;
    const select = tierField.querySelector("select");
    if (select) select.required = membersOnly;
}

audienceFields.forEach((field) => field.addEventListener("change", updateTierField));
updateTierField();

const imageInput = document.querySelector("[data-image-input]");
const imagePreview = document.querySelector("[data-image-preview]");
const imageName = document.querySelector("[data-image-name]");
const imageRemove = document.querySelector("[data-image-remove]");
const imageClear = document.querySelector("[data-image-clear]");

imageInput?.addEventListener("change", () => {
    const file = imageInput.files?.[0];
    if (!file) return;
    if (imageName) imageName.textContent = file.name;
    if (imageRemove) imageRemove.value = "false";
    if (imageClear) imageClear.hidden = false;
    if (!imagePreview) return;
    const reader = new FileReader();
    reader.addEventListener("load", () => {
        imagePreview.src = reader.result;
        imagePreview.hidden = false;
    }, { once: true });
    reader.readAsDataURL(file);
});

imageClear?.addEventListener("click", () => {
    if (imageInput) imageInput.value = "";
    if (imagePreview) { imagePreview.removeAttribute("src"); imagePreview.hidden = true; }
	if (imageName) imageName.textContent = document.querySelector(".composer-media") ? "Add a photo" : "Choose image";
    if (imageRemove) imageRemove.value = "true";
    imageClear.hidden = true;
});
