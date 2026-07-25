(function () {
    "use strict";

    function updateCharacterCounter(textarea) {
        var form = textarea.closest(".review-reply-form");
        if (!form) return;

        var counter = form.querySelector(".review-response-count");
        if (!counter) return;

        var maximumLength = textarea.maxLength > 0 ? textarea.maxLength : 1000;
        var currentLength = textarea.value.length;
        var nearLimit = Math.max(0, Math.floor(maximumLength * 0.9));

        counter.textContent = currentLength + "/" + maximumLength;
        counter.classList.toggle(
            "is-near-limit",
            currentLength >= nearLimit && currentLength < maximumLength
        );
        counter.classList.toggle("is-limit-reached", currentLength >= maximumLength);
    }

    function bindCharacterCounter(textarea) {
        if (textarea.dataset.characterCounterBound === "true") {
            updateCharacterCounter(textarea);
            return;
        }

        textarea.dataset.characterCounterBound = "true";
        textarea.addEventListener("input", function () {
            updateCharacterCounter(textarea);
        });
        textarea.addEventListener("change", function () {
            updateCharacterCounter(textarea);
        });
        updateCharacterCounter(textarea);
    }

    function initializeReviewCharacterCounters() {
        document.querySelectorAll(".review-response-input").forEach(bindCharacterCounter);
    }

    if (document.readyState === "loading") {
        document.addEventListener(
            "DOMContentLoaded",
            initializeReviewCharacterCounters,
            { once: true }
        );
    } else {
        initializeReviewCharacterCounters();
    }
})();
