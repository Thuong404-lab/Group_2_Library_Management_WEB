(function () {
    "use strict";

    var nativeSubmit = HTMLFormElement.prototype.submit;
    var activeForms = new Set();
    var formStates = new WeakMap();
    var pendingSubmissions = new WeakMap();

    function findSubmitter(form) {
        return form.querySelector("button:not([type]), button[type='submit'], input[type='submit'], input[type='image']");
    }

    function spinnerIn(button) {
        if (!(button instanceof HTMLElement)) return null;
        return button.querySelector(".spinner-border, .spinner-grow, [data-loading-spinner]");
    }

    function start(form, submitter, initialState) {
        if (!(form instanceof HTMLFormElement)
            || form.dataset.noLoadingState === "true"
            || formStates.has(form)) {
            return;
        }

        var button = submitter instanceof HTMLElement ? submitter : findSubmitter(form);
        var state = {
            button: button,
            buttonWasDisabled: initialState ? initialState.buttonWasDisabled : Boolean(button && button.disabled),
            restoreButtonDisabled: Boolean(initialState),
            buttonDisabledByLoading: Boolean(button && !button.disabled),
            buttonHadAriaLabel: Boolean(button && button.hasAttribute("aria-label")),
            buttonAriaLabel: button ? button.getAttribute("aria-label") : null,
            existingSpinner: null,
            existingSpinnerWasHidden: false,
            injectedSpinner: null
        };

        form.setAttribute("aria-busy", "true");
        formStates.set(form, state);
        activeForms.add(form);

        if (!(button instanceof HTMLButtonElement)) return;

        button.disabled = true;
        button.classList.add("app-submit-loading");
        var loadingMessage = document.documentElement.dataset.loadingMessage;
        if (loadingMessage) button.setAttribute("aria-label", loadingMessage);

        var existingSpinner = spinnerIn(button);
        if (existingSpinner) {
            state.existingSpinner = existingSpinner;
            state.existingSpinnerWasHidden = initialState
                ? initialState.spinnerWasHidden
                : existingSpinner.classList.contains("d-none");
            existingSpinner.classList.remove("d-none");
            existingSpinner.setAttribute("aria-hidden", "true");
            return;
        }

        var spinner = document.createElement("span");
        spinner.className = "spinner-border spinner-border-sm app-submit-spinner";
        spinner.setAttribute("role", "status");
        spinner.setAttribute("aria-hidden", "true");
        spinner.dataset.loadingSpinner = "true";
        button.appendChild(spinner);
        state.injectedSpinner = spinner;
    }

    function stop(form) {
        var state = formStates.get(form);
        if (!state) return;

        form.removeAttribute("aria-busy");
        if (state.button instanceof HTMLButtonElement) {
            state.button.classList.remove("app-submit-loading");
            if (state.restoreButtonDisabled) {
                state.button.disabled = state.buttonWasDisabled;
            } else if (state.buttonDisabledByLoading) {
                state.button.disabled = false;
            }
            if (state.buttonHadAriaLabel) {
                state.button.setAttribute("aria-label", state.buttonAriaLabel);
            } else {
                state.button.removeAttribute("aria-label");
            }
        }
        if (state.injectedSpinner) state.injectedSpinner.remove();
        if (state.existingSpinner && state.existingSpinnerWasHidden) {
            state.existingSpinner.classList.add("d-none");
        }

        formStates.delete(form);
        activeForms.delete(form);
    }

    document.addEventListener("submit", function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) return;

        var submitter = event.submitter || findSubmitter(form);
        var existingSpinner = spinnerIn(submitter);
        pendingSubmissions.set(form, {
            submitter: submitter,
            buttonWasDisabled: Boolean(submitter && submitter.disabled),
            spinnerWasHidden: Boolean(existingSpinner && existingSpinner.classList.contains("d-none"))
        });
    }, true);

    document.addEventListener("submit", function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) return;

        var pendingState = pendingSubmissions.get(form);
        pendingSubmissions.delete(form);
        if (!event.defaultPrevented) {
            start(form, pendingState ? pendingState.submitter : event.submitter, pendingState);
        }
    });

    HTMLFormElement.prototype.submit = function () {
        start(this, findSubmitter(this));
        return nativeSubmit.call(this);
    };

    window.addEventListener("pageshow", function () {
        Array.from(activeForms).forEach(stop);
    });

    window.AppFormLoading = {
        start: start,
        stop: stop
    };
})();
