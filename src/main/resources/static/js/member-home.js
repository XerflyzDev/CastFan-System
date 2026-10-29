const toast = document.querySelector("#app-toast");
const search = document.querySelector("#cast-search");
const eventCarousel = document.querySelector("[data-event-carousel]");
const eventSlides = [...document.querySelectorAll("[data-event-slide]")];
const eventDots = [...document.querySelectorAll("[data-event-dot]")];
const searchableItems = [...document.querySelectorAll("[data-search-item]")];
const workingSearchEmpty = document.querySelector("#working-search-empty");
const dragScrollers = [...document.querySelectorAll("[data-drag-scroll]")];
let toastTimer;
let eventScrollFrame;

function showMessage(message) {
    if (!toast || !message) {
        return;
    }
    toast.textContent = message;
    toast.classList.add("visible");
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => toast.classList.remove("visible"), 1800);
}

function setActiveEvent(index) {
    eventDots.forEach((dot, dotIndex) => {
        const isActive = dotIndex === index;
        dot.classList.toggle("active", isActive);
        dot.toggleAttribute("aria-current", isActive);
    });
}

function applySearch() {
    const query = search.value.trim().toLocaleLowerCase("en");
    let visibleCast = 0;

    searchableItems.forEach((item) => {
        const matches = item.dataset.searchItem.toLocaleLowerCase("en").includes(query);
        item.hidden = !matches;
        if (matches && item.classList.contains("cast-card")) {
            visibleCast += 1;
        }
    });

    if (workingSearchEmpty) {
        workingSearchEmpty.hidden = query.length === 0 || visibleCast > 0;
    }
}

document.querySelectorAll("[data-message]").forEach((control) => {
    control.addEventListener("click", () => showMessage(control.dataset.message));
});

document.querySelector("[data-clear-search]")?.addEventListener("click", () => {
    search.value = "";
    search.focus();
    applySearch();
});

search?.addEventListener("input", applySearch);

dragScrollers.forEach((scroller) => {
    let activePointer = null;
    let startX = 0;
    let startScrollLeft = 0;
    let dragged = false;
    let suppressClick = false;

    scroller.addEventListener("dragstart", (event) => event.preventDefault());
    scroller.addEventListener("pointerdown", (event) => {
        if (event.pointerType !== "mouse" || event.button !== 0) {
            return;
        }
        activePointer = event.pointerId;
        startX = event.clientX;
        startScrollLeft = scroller.scrollLeft;
        dragged = false;
        scroller.classList.add("is-dragging");
        scroller.setPointerCapture?.(event.pointerId);
    });
    scroller.addEventListener("pointermove", (event) => {
        if (activePointer !== event.pointerId) {
            return;
        }
        const distance = event.clientX - startX;
        if (Math.abs(distance) > 4) {
            dragged = true;
            event.preventDefault();
        }
        scroller.scrollLeft = startScrollLeft - distance;
    });

    const endDrag = (event) => {
        if (activePointer !== event.pointerId) {
            return;
        }
        if (dragged) {
            suppressClick = true;
        }
        scroller.releasePointerCapture?.(event.pointerId);
        activePointer = null;
        scroller.classList.remove("is-dragging");
        window.setTimeout(() => { suppressClick = false; }, 0);
    };

    scroller.addEventListener("pointerup", endDrag);
    scroller.addEventListener("pointercancel", endDrag);
    scroller.addEventListener("click", (event) => {
        if (suppressClick) {
            event.preventDefault();
            event.stopPropagation();
        }
    }, true);
});

eventDots.forEach((dot, index) => {
    dot.addEventListener("click", () => {
        eventCarousel?.scrollTo({
            left: eventSlides[index].offsetLeft - eventCarousel.offsetLeft,
            behavior: "smooth"
        });
    });
});

eventCarousel?.addEventListener("scroll", () => {
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
