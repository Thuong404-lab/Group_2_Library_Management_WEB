document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-auto-filter-form]').forEach(function (filterForm) {
        const focusStorageKey = filterForm.dataset.focusKey;

        if (focusStorageKey) {
            const savedFocusName = window.sessionStorage.getItem(focusStorageKey);
            if (savedFocusName) {
                const savedFocusField = filterForm.elements.namedItem(savedFocusName);
                window.sessionStorage.removeItem(focusStorageKey);
                if (savedFocusField) {
                    savedFocusField.focus();
                    if (typeof savedFocusField.setSelectionRange === 'function') {
                        const caretPosition = savedFocusField.value.length;
                        savedFocusField.setSelectionRange(caretPosition, caretPosition);
                    }
                }
            }
        }

        const submitFilter = function () {
            if (typeof filterForm.requestSubmit === 'function') {
                filterForm.requestSubmit();
                return;
            }
            filterForm.submit();
        };

        filterForm.querySelectorAll('[data-auto-submit]').forEach(function (control) {
            control.addEventListener('change', submitFilter);
        });

        filterForm.querySelectorAll('[data-auto-search]').forEach(function (control) {
            let searchTimer;
            control.addEventListener('input', function () {
                window.clearTimeout(searchTimer);
                searchTimer = window.setTimeout(function () {
                    if (focusStorageKey) {
                        window.sessionStorage.setItem(focusStorageKey, control.name);
                    }
                    submitFilter();
                }, 600);
            });
        });
    });
});
