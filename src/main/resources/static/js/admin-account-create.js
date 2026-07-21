document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll(".admin-create-account-form").forEach(function (form) {
        const modal = form.closest(".modal");
        const submitButton = form.querySelector("button[type='submit']");
        const globalError = form.querySelector(".create-account-global-error");
        const fields = Array.from(form.querySelectorAll(".validate-field"));
        let submitting = false;

        function feedbackFor(field) {
            return form.querySelector("[data-error-for='" + field.name + "']");
        }

        function clearFieldState(field) {
            field.classList.remove("is-invalid", "is-valid");
            const feedback = feedbackFor(field);
            if (feedback != null) {
                feedback.textContent = "";
                feedback.classList.remove("d-block");
            }
        }

        function clearValidation() {
            fields.forEach(clearFieldState);
            globalError.textContent = "";
            globalError.classList.add("d-none");
        }

        function showValidation(errors) {
            let firstInvalidField = null;
            const globalMessages = [];

            fields.forEach(function (field) {
                const message = errors[field.name];
                field.classList.remove("is-invalid", "is-valid");
                field.classList.add(message == null ? "is-valid" : "is-invalid");

                const feedback = feedbackFor(field);
                if (feedback != null) {
                    feedback.textContent = message || "";
                    feedback.classList.toggle("d-block", message != null);
                }
                if (message != null && firstInvalidField == null) firstInvalidField = field;
            });

            Object.entries(errors).forEach(function (entry) {
                if (form.elements.namedItem(entry[0]) == null) globalMessages.push(entry[1]);
            });
            if (globalMessages.length > 0) {
                globalError.textContent = globalMessages.join(" ");
                globalError.classList.remove("d-none");
            }
            if (firstInvalidField != null) firstInvalidField.focus();
        }

        fields.forEach(function (field) {
            field.addEventListener("input", function () { clearFieldState(field); });
            field.addEventListener("change", function () { clearFieldState(field); });
        });

        const passwordToggle = form.querySelector(".create-password-toggle");
        if (passwordToggle != null) {
            passwordToggle.addEventListener("click", function () {
                const password = form.elements.password;
                const showPassword = password.type === "password";
                password.type = showPassword ? "text" : "password";
                passwordToggle.querySelector("i").className = showPassword
                    ? "bi bi-eye text-muted"
                    : "bi bi-eye-slash text-muted";
                const label = showPassword
                    ? passwordToggle.dataset.hideLabel
                    : passwordToggle.dataset.showLabel;
                passwordToggle.setAttribute("aria-label", label);
                passwordToggle.setAttribute("title", label);
            });
        }

        form.addEventListener("submit", async function (event) {
            event.preventDefault();
            if (submitting) return;

            submitting = true;
            submitButton.disabled = true;
            clearValidation();
            try {
                const params = new URLSearchParams();
                new FormData(form).forEach(function (value, key) {
                    if (!key.startsWith("_csrf")) params.append(key, value);
                });
                const validationUrl = new URL(form.dataset.validationUrl, window.location.origin);
                params.forEach(function (value, key) {
                    validationUrl.searchParams.append(key, value);
                });

                const response = await fetch(validationUrl.toString(), {
                    headers: { "Accept": "application/json" }
                });
                const contentType = response.headers.get("content-type") || "";
                if (!response.ok || !contentType.includes("application/json")) {
                    throw new Error("Validation request failed");
                }

                const errors = await response.json();
                if (Object.keys(errors).length > 0) {
                    showValidation(errors);
                    return;
                }
                form.submit();
            } catch (error) {
                globalError.textContent = form.dataset.validationUnavailableMessage;
                globalError.classList.remove("d-none");
            } finally {
                submitButton.disabled = false;
                submitting = false;
            }
        });

        if (modal != null) {
            modal.addEventListener("hidden.bs.modal", function () {
                form.reset();
                clearValidation();
                if (form.elements.password != null) form.elements.password.type = "password";
                if (passwordToggle != null) {
                    passwordToggle.querySelector("i").className = "bi bi-eye-slash text-muted";
                    passwordToggle.setAttribute("aria-label", passwordToggle.dataset.showLabel);
                    passwordToggle.setAttribute("title", passwordToggle.dataset.showLabel);
                }
            });
        }
    });
});
