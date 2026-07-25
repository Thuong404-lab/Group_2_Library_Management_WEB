(function () {
    "use strict";

    var hideTimer = null;
    var icons = {
        success: "bi bi-check-lg",
        error: "bi bi-exclamation-lg",
        warning: "bi bi-exclamation-triangle",
        info: "bi bi-info-lg"
    };

    function refs() {
        var region = document.getElementById("appFeedbackRegion");
        var toast = document.getElementById("customToast");
        if (!region || !toast) return null;
        return {
            region: region,
            toast: toast,
            title: document.getElementById("toastTitle"),
            message: document.getElementById("toastMessage"),
            icon: toast.querySelector(".app-feedback-toast__icon i")
        };
    }

    function normalizeMessage(value) {
        return String(value || "").replace(/\s+/g, " ").trim();
    }

    function hide() {
        var elements = refs();
        if (!elements || elements.toast.hidden) return;
        elements.toast.classList.remove("is-visible");
        window.setTimeout(function () {
            elements.toast.hidden = true;
        }, 210);
    }

    function show(message, tone, options) {
        var elements = refs();
        var normalizedMessage = normalizeMessage(message);
        if (!elements || !normalizedMessage) return;

        var normalizedTone = icons[tone] ? tone : "info";
        var settings = options || {};
        window.clearTimeout(hideTimer);

        elements.toast.className = "app-feedback-toast is-" + normalizedTone;
        elements.title.textContent = settings.title
            || elements.region.dataset[normalizedTone + "Title"]
            || elements.region.dataset.infoTitle;
        elements.message.textContent = normalizedMessage;
        elements.icon.className = icons[normalizedTone];
        elements.toast.setAttribute("role", normalizedTone === "error" ? "alert" : "status");
        elements.region.setAttribute("aria-live", normalizedTone === "error" ? "assertive" : "polite");
        elements.toast.hidden = false;
        window.requestAnimationFrame(function () {
            elements.toast.classList.add("is-visible");
        });

        if (settings.persistent !== true) {
            hideTimer = window.setTimeout(hide, settings.duration || 5000);
        }
    }

    function hideMatchingLegacyFeedback(message) {
        var normalizedMessage = normalizeMessage(message);
        if (!normalizedMessage) return;

        document.querySelectorAll([
            ".alert-success",
            ".alert-danger",
            ".alert-warning[data-app-feedback]",
            ".alert-info[data-app-feedback]",
            ".app-alert--success",
            ".app-alert--danger",
            ".finance-alert--success",
            ".finance-alert--danger",
            ".auth-alert.alert-warning",
            ".auth-alert.alert-info"
        ].join(",")).forEach(function (element) {
            if (element.closest(".modal") != null) return;
            if (normalizeMessage(element.textContent) !== normalizedMessage) return;
            element.hidden = true;
            element.setAttribute("aria-hidden", "true");
        });
    }

    function collectFeedback() {
        var sources = Array.from(document.querySelectorAll([
            "[data-app-feedback]",
            ".alert-success",
            ".alert-danger",
            ".app-alert--success",
            ".app-alert--danger",
            ".finance-alert--success",
            ".finance-alert--danger",
            ".auth-alert.alert-warning",
            ".auth-alert.alert-info"
        ].join(",")))
            .filter(function (source) {
                if (normalizeMessage(source.textContent).length === 0) return false;
                if (source.hasAttribute("data-app-feedback")) return true;
                return source.hidden !== true
                    && !source.classList.contains("d-none")
                    && source.getAttribute("aria-hidden") !== "true"
                    && source.closest(".modal,[hidden]") === null;
            });

        if (sources.length === 0) return null;
        return sources.find(function (source) {
            return feedbackTone(source) === "error";
        }) || sources.find(function (source) {
            return feedbackTone(source) === "warning";
        }) || sources[0];
    }

    function feedbackTone(source) {
        if (source.dataset.appFeedback && icons[source.dataset.appFeedback]) {
            return source.dataset.appFeedback;
        }
        if (source.matches(".alert-danger,.app-alert--danger,.finance-alert--danger")) {
            return "error";
        }
        if (source.matches(".alert-warning")) {
            return "warning";
        }
        if (source.matches(".alert-info")) {
            return "info";
        }
        return "success";
    }

    window.AppFeedback = {
        show: show,
        hide: hide,
        success: function (message, options) { show(message, "success", options); },
        error: function (message, options) { show(message, "error", options); },
        warning: function (message, options) { show(message, "warning", options); },
        info: function (message, options) { show(message, "info", options); }
    };

    document.addEventListener("DOMContentLoaded", function () {
        var closeButton = document.querySelector(".app-feedback-toast__close");
        if (closeButton) closeButton.addEventListener("click", hide);

        var selected = collectFeedback();
        if (!selected) return;

        var tone = feedbackTone(selected);
        var message = normalizeMessage(selected.textContent);
        hideMatchingLegacyFeedback(message);
        show(message, tone);
    });
})();
