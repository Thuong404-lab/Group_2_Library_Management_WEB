(function () {
    "use strict";

    function rawAmount(value) {
        return String(value || "").replace(/\D/g, "").replace(/^0+(?=\d)/, "");
    }

    function formatAmount(value) {
        const digits = rawAmount(value);
        return digits.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
    }

    function validateAmount(input) {
        const digits = rawAmount(input.value);
        const amount = digits === "" ? 0 : Number(digits);
        const minimum = Number(input.dataset.minAmount || 0);
        const maximum = Number(input.dataset.maxAmount || 0);

        if (digits !== "" && minimum > 0 && amount < minimum) {
            input.setCustomValidity((input.dataset.minMessage || "Minimum amount: {0} VND.").replace("{0}", formatAmount(minimum)));
        } else if (maximum > 0 && amount > maximum) {
            input.setCustomValidity((input.dataset.maxMessage || "Maximum amount: {0} VND.").replace("{0}", formatAmount(maximum)));
        } else {
            input.setCustomValidity("");
        }
        return digits;
    }

    document.querySelectorAll("[data-vnd-input]").forEach(function (input) {
        function refresh() {
            input.value = formatAmount(input.value);
            validateAmount(input);
        }

        input.addEventListener("input", refresh);
        refresh();

        if (input.form != null) {
            input.form.addEventListener("submit", function (event) {
                const digits = validateAmount(input);
                if (!input.checkValidity()) {
                    event.preventDefault();
                    input.reportValidity();
                    return;
                }
                input.value = digits;
            });
        }
    });
})();
