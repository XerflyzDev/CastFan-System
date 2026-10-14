const toast = document.querySelector(".concept-toast");
const search = document.querySelector("#concept-search");
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

document.querySelector("[data-clear-search]").addEventListener("click", () => {
    search.value = "";
    search.focus();
});

const eventCarousel = document.querySelector("[data-event-carousel]");
const eventSlides = [...document.querySelectorAll("[data-event-slide]")];
const eventDots = [...document.querySelectorAll("[data-event-dot]")];
let eventScrollFrame;

function setActiveEvent(index) {
    eventDots.forEach((dot, dotIndex) => {
        const isActive = dotIndex === index;
        dot.classList.toggle("active", isActive);
        dot.toggleAttribute("aria-current", isActive);
    });
}

eventDots.forEach((dot, index) => {
    dot.addEventListener("click", () => {
        eventCarousel.scrollTo({
            left: eventSlides[index].offsetLeft - eventCarousel.offsetLeft,
            behavior: "smooth"
        });
    });
});

eventCarousel.addEventListener("scroll", () => {
    window.cancelAnimationFrame(eventScrollFrame);
    eventScrollFrame = window.requestAnimationFrame(() => {
        const activeIndex = eventSlides.reduce((closestIndex, slide, index) => {
            const currentDistance = Math.abs(slide.offsetLeft - eventCarousel.scrollLeft);
            const closestDistance = Math.abs(eventSlides[closestIndex].offsetLeft - eventCarousel.scrollLeft);
            return currentDistance < closestDistance ? index : closestIndex;
        }, 0);
        setActiveEvent(activeIndex);
    });
});
