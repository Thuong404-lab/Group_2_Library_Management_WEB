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
            if (globalError != null) {
                globalError.textContent = "";
                globalError.classList.add("d-none");
            }
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
                if (globalError != null) {
                    globalError.textContent = globalMessages.join(" ");
                    globalError.classList.remove("d-none");
                }
            }
            if (firstInvalidField != null) firstInvalidField.focus();
        }

        fields.forEach(function (field) {
            field.addEventListener("input", function () { clearFieldState(field); });
            field.addEventListener("change", function () { clearFieldState(field); });
        });

        function validateLocalFields() {
            const errors = {};
            fields.forEach(function (field) {
                const value = field.value == null ? "" : field.value.trim();
                if (field.required && value === "" && field.dataset.requiredMessage) {
                    errors[field.name] = field.dataset.requiredMessage;
                } else if (!field.checkValidity() && field.dataset.invalidMessage) {
                    errors[field.name] = field.dataset.invalidMessage;
                }
            });

            const password = form.elements.namedItem("password");
            const confirmation = form.elements.namedItem("confirmPassword");
            if (password != null && confirmation != null
                    && confirmation.value !== "" && password.value !== confirmation.value
                    && form.dataset.passwordMismatchMessage) {
                errors.confirmPassword = form.dataset.passwordMismatchMessage;
            }
            return errors;
        }

        form.querySelectorAll(".create-password-toggle").forEach(function (passwordToggle) {
            passwordToggle.addEventListener("click", function () {
                const inputGroup = passwordToggle.closest(".position-relative");
                const passwordInput = inputGroup ? inputGroup.querySelector("input[name='password'], input[name='confirmPassword']") : null;
                if (passwordInput != null) {
                    const showPassword = passwordInput.type === "password";
                    passwordInput.type = showPassword ? "text" : "password";
                    passwordToggle.querySelector("i").className = showPassword
                        ? "bi bi-eye text-muted"
                        : "bi bi-eye-slash text-muted";
                    const label = showPassword
                        ? passwordToggle.dataset.hideLabel
                        : passwordToggle.dataset.showLabel;
                    passwordToggle.setAttribute("aria-label", label);
                    passwordToggle.setAttribute("title", label);
                }
            });
        });

        form.addEventListener("submit", async function (event) {
            const localErrors = validateLocalFields();
            if (Object.keys(localErrors).length > 0) {
                event.preventDefault();
                showValidation(localErrors);
                return;
            }

            // Forms without a remote validation URL continue through their
            // normal POST flow after client-side validation succeeds.
            if (!form.dataset.validationUrl) return;

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
                if (globalError != null) {
                    globalError.textContent = form.dataset.validationUnavailableMessage;
                    globalError.classList.remove("d-none");
                }
            } finally {
                submitButton.disabled = false;
                submitting = false;
            }
        });

        if (modal != null) {
            modal.addEventListener("hidden.bs.modal", function () {
                form.reset();
                clearValidation();
                form.querySelectorAll("input[name='password'], input[name='confirmPassword']").forEach(function(inp) {
                    inp.type = "password";
                });
                form.querySelectorAll(".create-password-toggle").forEach(function(passwordToggle) {
                    passwordToggle.querySelector("i").className = "bi bi-eye-slash text-muted";
                    passwordToggle.setAttribute("aria-label", passwordToggle.dataset.showLabel);
                    passwordToggle.setAttribute("title", passwordToggle.dataset.showLabel);
                });
            });
        }
    });
});
