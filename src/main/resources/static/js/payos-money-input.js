(function () {
    "use strict";

    function rawAmount(value) {
        return String(value || "").replace(/\D/g, "").replace(/^0+(?=\d)/, "");
    }

    function formatAmount(value) {
        const digits = rawAmount(value);
        const language = document.documentElement.lang || "en";
        const separator = new Intl.NumberFormat(language).format(1000).replace(/[01]/g, "") || ",";
        return digits.replace(/\B(?=(\d{3})+(?!\d))/g, separator);
    }

    function validateAmount(input) {
        const digits = rawAmount(input.value);
        const amount = digits === "" ? 0 : Number(digits);
        const minimum = Number(input.dataset.minAmount || 0);
        const maximum = Number(input.dataset.maxAmount || 0);
        const currentBalance = Number(input.dataset.currentBalance || 0);
        const maximumBalance = Number(input.dataset.maxBalance || 0);

        if (digits === "" && input.required) {
            input.setCustomValidity(input.dataset.requiredMessage || "Please enter an amount.");
        } else if (digits !== "" && minimum > 0 && amount < minimum) {
            input.setCustomValidity((input.dataset.minMessage || "Minimum amount: {0} VND.").replace("{0}", formatAmount(minimum)));
        } else if (maximum > 0 && amount > maximum) {
            input.setCustomValidity((input.dataset.maxMessage || "Maximum amount: {0} VND.").replace("{0}", formatAmount(maximum)));
        } else if (maximumBalance > 0 && currentBalance + amount > maximumBalance) {
            input.setCustomValidity((input.dataset.balanceMessage || "The wallet balance cannot exceed {0} VND.")
                .replace("{0}", formatAmount(maximumBalance)));
        } else {
            input.setCustomValidity("");
        }
        return digits;
    }

    document.querySelectorAll("[data-vnd-input]").forEach(function (input) {
        function errorElement() {
            const selector = input.dataset.errorTarget;
            return selector ? document.querySelector(selector) : null;
        }

        function renderValidity(showError) {
            const error = errorElement();
            const invalid = !input.checkValidity();
            input.classList.toggle("is-invalid", showError && invalid);
            input.setAttribute("aria-invalid", showError && invalid ? "true" : "false");
            if (error != null) {
                error.textContent = showError && invalid
                    ? input.validationMessage
                    : (error.dataset.defaultMessage || "");
                error.classList.toggle("is-visible", showError && invalid);
            }
        }

        function refresh() {
            input.value = formatAmount(input.value);
            validateAmount(input);
            if (input.dataset.validationVisible === "true") {
                renderValidity(true);
            }
        }

        input.addEventListener("input", refresh);
        input.addEventListener("blur", function () {
            input.dataset.validationVisible = "true";
            validateAmount(input);
            renderValidity(true);
        });
        refresh();

        if (input.form != null) {
            input.form.addEventListener("submit", function (event) {
                const digits = validateAmount(input);
                if (!input.checkValidity()) {
                    event.preventDefault();
                    input.dataset.validationVisible = "true";
                    renderValidity(true);
                    if (errorElement() == null) {
                        input.reportValidity();
                    }
                    input.focus();
                    return;
                }
                renderValidity(false);
                input.value = digits;
            });

            input.form.addEventListener("reset", function () {
                window.requestAnimationFrame(function () {
                    delete input.dataset.validationVisible;
                    input.setCustomValidity("");
                    renderValidity(false);
                });
            });
        }
    });
})();
