(() => {
const toast = document.querySelector("#app-toast");
const avatarInput = document.querySelector("[data-avatar-input]");
const avatarForm = document.querySelector("[data-avatar-form]");
const cropDialog = document.querySelector("[data-avatar-crop-dialog]");
const cropCanvas = document.querySelector("[data-crop-canvas]");
const cropStage = document.querySelector("[data-crop-stage]");
const cropZoom = document.querySelector("[data-crop-zoom]");
const cropSave = document.querySelector("[data-crop-save]");
const cropContext = cropCanvas?.getContext("2d");
let toastTimer;
let cropImage;
let cropBaseScale = 1;
let cropScale = 1;
let cropOffsetX = 0;
let cropOffsetY = 0;
let pointerStart;

function clampCropOffset() {
    if (!cropImage || !cropCanvas) return;
    const renderedWidth = cropImage.naturalWidth * cropBaseScale * cropScale;
    const renderedHeight = cropImage.naturalHeight * cropBaseScale * cropScale;
    cropOffsetX = Math.max((cropCanvas.width - renderedWidth) / 2, Math.min((renderedWidth - cropCanvas.width) / 2, cropOffsetX));
    cropOffsetY = Math.max((cropCanvas.height - renderedHeight) / 2, Math.min((renderedHeight - cropCanvas.height) / 2, cropOffsetY));
}

function renderCrop() {
    if (!cropImage || !cropCanvas || !cropContext) return;
    clampCropOffset();
    const width = cropImage.naturalWidth * cropBaseScale * cropScale;
    const height = cropImage.naturalHeight * cropBaseScale * cropScale;
    cropContext.clearRect(0, 0, cropCanvas.width, cropCanvas.height);
    cropContext.drawImage(
        cropImage,
        (cropCanvas.width - width) / 2 + cropOffsetX,
        (cropCanvas.height - height) / 2 + cropOffsetY,
        width,
        height
    );
}

function resetCrop() {
    cropImage = undefined;
    pointerStart = undefined;
    if (avatarInput) avatarInput.value = "";
    if (cropZoom) cropZoom.value = "1";
}

function closeCrop() {
    cropDialog?.close();
    resetCrop();
}

avatarInput?.addEventListener("change", (event) => {
    const file = event.target.files?.[0];
    if (!file || !cropDialog || !cropCanvas || !cropContext) return;

    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
        showMessage("Choose a JPG, PNG, or WebP image.");
        resetCrop();
        return;
    }

    const reader = new FileReader();
    reader.addEventListener("load", () => {
        const image = new Image();
        image.addEventListener("load", () => {
            cropImage = image;
            cropBaseScale = Math.max(cropCanvas.width / image.naturalWidth, cropCanvas.height / image.naturalHeight);
            cropScale = 1;
            cropOffsetX = 0;
            cropOffsetY = 0;
            cropZoom.value = "1";
            renderCrop();
            cropDialog.showModal();
        }, { once: true });
        image.addEventListener("error", () => {
            showMessage("This image could not be opened.");
            resetCrop();
        }, { once: true });
        image.src = reader.result;
    }, { once: true });
    reader.addEventListener("error", () => {
        showMessage("This image could not be opened.");
        resetCrop();
    }, { once: true });
    reader.readAsDataURL(file);
});

cropZoom?.addEventListener("input", (event) => {
    cropScale = Number(event.target.value);
    renderCrop();
});

cropStage?.addEventListener("pointerdown", (event) => {
    if (!cropImage) return;
    cropStage.setPointerCapture(event.pointerId);
    cropStage.classList.add("dragging");
    pointerStart = { x: event.clientX, y: event.clientY, offsetX: cropOffsetX, offsetY: cropOffsetY };
});

cropStage?.addEventListener("pointermove", (event) => {
    if (!pointerStart || !cropCanvas) return;
    const scale = cropCanvas.width / cropStage.clientWidth;
    cropOffsetX = pointerStart.offsetX + (event.clientX - pointerStart.x) * scale;
    cropOffsetY = pointerStart.offsetY + (event.clientY - pointerStart.y) * scale;
    renderCrop();
});

function endCropDrag() {
    pointerStart = undefined;
    cropStage?.classList.remove("dragging");
}

cropStage?.addEventListener("pointerup", endCropDrag);
cropStage?.addEventListener("pointercancel", endCropDrag);
document.querySelectorAll("[data-crop-cancel]").forEach((button) => button.addEventListener("click", closeCrop));
cropDialog?.addEventListener("cancel", (event) => {
    event.preventDefault();
    closeCrop();
});

cropSave?.addEventListener("click", () => {
    if (!cropCanvas || !avatarInput || !avatarForm || !cropSave) return;
    cropSave.disabled = true;
    cropSave.querySelector("span").textContent = "Saving...";
    cropCanvas.toBlob((blob) => {
        if (!blob) {
            cropSave.disabled = false;
            cropSave.querySelector("span").textContent = "Save photo";
            showMessage("Unable to prepare this photo.");
            return;
        }
        const transfer = new DataTransfer();
        transfer.items.add(new File([blob], "profile-photo.jpg", { type: "image/jpeg" }));
        avatarInput.files = transfer.files;
        avatarForm.requestSubmit();
    }, "image/jpeg", 0.9);
});

function showMessage(message) {
    if (!toast || !message) return;
    toast.textContent = message;
    toast.classList.add("visible");
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => toast.classList.remove("visible"), 1800);
}

const pageMessages = new Map([
    ["saved", "Profile saved."],
    ["nameSaved", "Account name updated."],
    ["photoSaved", "Profile photo updated."],
    ["photoRemoved", "Profile photo removed."]
]);
const pageParameters = new URLSearchParams(window.location.search);
for (const [parameter, message] of pageMessages) {
    if (pageParameters.has(parameter)) {
        showMessage(message);
        break;
    }
}

document.querySelectorAll("[data-message]").forEach((control) => {
    control.addEventListener("click", () => showMessage(control.dataset.message));
});

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
    if (imageName) imageName.textContent = "Choose photo";
    if (imageRemove) imageRemove.value = "true";
    imageClear.hidden = true;
});

document
    .querySelectorAll("[data-notification-preference]")
    .forEach((toggle) => {
        toggle.addEventListener("change", async () => {
            const previousValue = !toggle.checked;
            const form = toggle.closest("form");
            const csrfInput = form?.querySelector('input[name="_csrf"]');

            if (!form || !csrfInput?.value) {
                toggle.checked = previousValue;
                alert("Unable to save notification preference. Please reload the page.");
                return;
            }

            toggle.disabled = true;

            try {
                const response = await fetch(
                    "/member/profile/notification-preferences",
                    {
                        method: "POST",
                        credentials: "same-origin",
                        headers: {
                            "Content-Type":
                                "application/x-www-form-urlencoded;charset=UTF-8",
                            "X-CSRF-TOKEN": csrfInput.value
                        },
                        body: new URLSearchParams({
                            preference: toggle.name,
                            enabled: String(toggle.checked)
                        })
                    }
                );

                if (!response.ok) {
                    throw new Error("Failed to save notification preference.");
                }
            } catch (error) {
                toggle.checked = previousValue;
                alert(
                    "Could not save your notification preference. Please try again."
                );
            } finally {
                toggle.disabled = false;
            }
        });
    });
})();
