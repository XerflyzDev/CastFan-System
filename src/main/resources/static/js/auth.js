(() => {
    const googleMock = document.querySelector("[data-google-login-mock]");
    const googleStatus = document.getElementById("google-login-status");
    googleMock?.addEventListener("click", () => {
        if (googleStatus) googleStatus.hidden = false;
    });

    const form = document.querySelector(".auth-form");
    const email = document.getElementById("email");
    const field = email?.closest(".form-field");
    const error = document.getElementById("email-format-error");

    const updateEmailState = () => {
        if (!email || !field) return;
        const isInvalid = email.value.length > 0 && !email.validity.valid;
        field.classList.toggle("has-format-error", isInvalid);
        email.setAttribute("aria-invalid", String(isInvalid));
        if (error) error.hidden = !isInvalid;
    };

    email?.addEventListener("input", updateEmailState);
    email?.addEventListener("blur", updateEmailState);

    const name = document.getElementById("displayName");
    const nameHint = document.querySelector("[data-name-validation]");
    const updateNameState = () => {
        if (!name || !nameHint) return;
        const characterCount = name.value.replace(/\s/g, "").length;
        nameHint.classList.toggle("is-invalid", characterCount > 0 && characterCount < 3);
        nameHint.classList.toggle("is-valid", characterCount >= 3);
        nameHint.textContent = characterCount >= 3 ? "Name length looks good." : "Use at least 3 characters.";
        name.setAttribute("aria-invalid", String(characterCount > 0 && characterCount < 3));
    };
    name?.addEventListener("input", updateNameState);

    const signupEmailHint = document.querySelector("[data-email-validation]");
    const updateSignupEmailState = () => {
        if (!signupEmailHint) return;
        const hasValue = email.value.length > 0;
        const isValid = hasValue && email.validity.valid;
        signupEmailHint.classList.toggle("is-invalid", hasValue && !isValid);
        signupEmailHint.classList.toggle("is-valid", isValid);
        signupEmailHint.textContent = isValid
            ? "Email format looks good."
            : hasValue ? "Enter a valid email such as name@example.com." : "Enter an email such as name@example.com.";
    };
    email?.addEventListener("input", updateSignupEmailState);

    const password = document.getElementById("password");
    const strength = document.querySelector("[data-password-strength]");
    const strengthLabel = document.querySelector("[data-password-strength-label]");
    const strengthHelp = document.querySelector("[data-password-strength-help]");
    const updatePasswordStrength = () => {
        if (!password || !strength || !strengthLabel || !strengthHelp) return;
        const value = password.value;
        strength.hidden = value.length === 0;
        const characterTypes = [/[a-z]/, /[A-Z]/, /\d/, /[^A-Za-z0-9]/]
            .filter((pattern) => pattern.test(value)).length;
        let level = "";
        let label = "Not rated";
        let help = "Use 8 to 72 characters.";

        if (value.length > 0) {
            level = "weak";
            label = "Weak";
            help = "Use at least 8 characters.";
        }
        if (value.length >= 8 && characterTypes >= 2) {
            level = "fair";
            label = "Fair";
            help = "Add length or mix letters, numbers, and symbols.";
        }
        if (value.length >= 12 && characterTypes >= 3) {
            level = "strong";
            label = "Strong";
            help = "Strong password.";
        }

        strength.classList.remove("weak", "fair", "strong");
        if (level) strength.classList.add(level);
        strengthLabel.textContent = label;
        strengthHelp.textContent = help;
    };
    password?.addEventListener("input", updatePasswordStrength);
    updatePasswordStrength();

    const storeName = document.getElementById("storeName");
    const storeSlug = document.getElementById("slug");
    let slugEdited = Boolean(storeSlug?.value);
    storeSlug?.addEventListener("input", () => { slugEdited = true; });
    storeName?.addEventListener("input", () => {
        if (!storeSlug || slugEdited) return;
        storeSlug.value = storeName.value.toLowerCase().normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9]+/g, "-")
            .replace(/^-|-$/g, "").slice(0, 100);
    });

    const storeSignupForm = document.querySelector(".store-signup-form");
    if (storeSignupForm) {
        const steps = [...storeSignupForm.querySelectorAll("[data-signup-step]")];
        const nextButton = storeSignupForm.querySelector("[data-step-next]");
        const backButton = storeSignupForm.querySelector("[data-step-back]");
        const stepLabel = document.querySelector("[data-step-label]");
        const stepTitle = document.querySelector("[data-step-title]");
        const progress = document.querySelector(".signup-progress-track > span");
        const confirmation = storeSignupForm.querySelector("#confirmPassword");
        let currentStep = Number(storeSignupForm.dataset.activeStep || "1") === 2 ? 2 : 1;

        const setStep = (stepNumber, moveFocus = false) => {
            currentStep = stepNumber;
            steps.forEach((step) => {
                const active = Number(step.dataset.signupStep) === currentStep;
                step.classList.toggle("is-active", active);
                step.setAttribute("aria-hidden", String(!active));
            });
            if (stepLabel) stepLabel.textContent = `Step ${currentStep} of 2`;
            if (stepTitle) stepTitle.textContent = currentStep === 1 ? "Owner account" : "Store information";
            progress?.classList.toggle("is-complete", currentStep === 2);
            if (moveFocus) {
                steps[currentStep - 1]?.querySelector("input, textarea")?.focus({preventScroll: true});
                document.querySelector(".store-signup-panel")?.scrollIntoView({behavior: "smooth", block: "start"});
            }
        };

        const validateOwnerStep = () => {
            const ownerStep = steps[0];
            const ownerFields = [...ownerStep.querySelectorAll("input")];
            if (password && confirmation) {
                confirmation.setCustomValidity(password.value === confirmation.value ? "" : "Passwords do not match");
            }
            const firstInvalid = ownerFields.find((input) => !input.checkValidity());
            if (firstInvalid) {
                firstInvalid.reportValidity();
                return false;
            }
            return true;
        };

        confirmation?.addEventListener("input", () => confirmation.setCustomValidity(""));
        nextButton?.addEventListener("click", () => {
            if (validateOwnerStep()) setStep(2, true);
        });
        backButton?.addEventListener("click", () => setStep(1, true));
        storeSignupForm.addEventListener("submit", (event) => {
            if (currentStep === 1) {
                event.preventDefault();
                if (validateOwnerStep()) setStep(2, true);
            }
        });

        storeSignupForm.classList.add("is-enhanced");
        setStep(currentStep);
    }
})();
