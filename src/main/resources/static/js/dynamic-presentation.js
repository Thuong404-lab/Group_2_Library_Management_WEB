(function () {
    "use strict";

    function applyNumericStyle(selector, dataKey, property, suffix) {
        document.querySelectorAll(selector).forEach(function (element) {
            var value = Number(element.dataset[dataKey]);
            if (Number.isFinite(value)) {
                element.style[property] = value + suffix;
            }
        });
    }

    function applyDynamicPresentation() {
        applyNumericStyle("[data-presentation-width]", "presentationWidth", "width", "%");
        applyNumericStyle("[data-presentation-height]", "presentationHeight", "height", "px");
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", applyDynamicPresentation);
    } else {
        applyDynamicPresentation();
    }
})();
