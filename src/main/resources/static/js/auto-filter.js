document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-auto-filter-form]').forEach(function (filterForm) {
        const focusStorageKey = filterForm.dataset.focusKey;

        const performAjaxFilter = function (activeControlName) {
            // Build request URL with current form values
            const formData = new FormData(filterForm);
            const params = new URLSearchParams();
            for (const [key, value] of formData.entries()) {
                if (value !== null && value !== '') {
                    params.append(key, value);
                }
            }
            const actionUrl = filterForm.action || window.location.pathname;
            const targetUrl = actionUrl + (params.toString() ? '?' + params.toString() : '');

            fetch(targetUrl, {
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.text();
            })
            .then(function (htmlText) {
                const parser = new DOMParser();
                const newDoc = parser.parseFromString(htmlText, 'text/html');

                // 1. Swap Summary Chips
                const currentChips = document.querySelector('.member-summary-chips, .member-list-summary');
                const newChips = newDoc.querySelector('.member-summary-chips, .member-list-summary');
                if (currentChips && newChips) {
                    currentChips.innerHTML = newChips.innerHTML;
                }

                // 2. Swap Table Container
                const currentTable = document.querySelector('.member-table-wrap, .member-list-table-wrap');
                const newTable = newDoc.querySelector('.member-table-wrap, .member-list-table-wrap');
                if (currentTable && newTable) {
                    currentTable.innerHTML = newTable.innerHTML;
                }

                // 3. Swap Pagination
                const currentPagination = document.querySelector('.member-pagination, .pagination-container, nav[aria-label="Pagination"]');
                const newPagination = newDoc.querySelector('.member-pagination, .pagination-container, nav[aria-label="Pagination"]');
                if (currentPagination && newPagination) {
                    currentPagination.innerHTML = newPagination.innerHTML;
                }

                // 4. Update browser URL without page reload
                window.history.replaceState(null, '', targetUrl);

                // 5. Remove old moved modals from body before appending new ones to prevent duplicate IDs
                document.querySelectorAll('body > .modal').forEach(function (oldModal) {
                    if (oldModal.id && (
                        oldModal.id.startsWith('deleteMemberModal') ||
                        oldModal.id.startsWith('deactivateMemberModal') ||
                        oldModal.id.startsWith('updateMemberModal') ||
                        oldModal.id.startsWith('resetPasswordMemberModal')
                    )) {
                        if (window.bootstrap && bootstrap.Modal) {
                            const instance = bootstrap.Modal.getInstance(oldModal);
                            if (instance) {
                                instance.dispose();
                            }
                        }
                        oldModal.remove();
                    }
                });

                // 6. Re-move any newly created modals to body
                document.querySelectorAll('.member-management-page .modal').forEach(function (modalEl) {
                    if (modalEl.parentElement !== document.body) {
                        document.body.appendChild(modalEl);
                    }
                });

                // 7. Re-initialize Tooltips if Bootstrap Tooltip is present
                if (window.bootstrap && bootstrap.Tooltip) {
                    document.querySelectorAll('.account-action-tooltip').forEach(function (btn) {
                        if (!bootstrap.Tooltip.getInstance(btn)) {
                            new bootstrap.Tooltip(btn, { trigger: 'hover' });
                        }
                    });
                }

                // 8. Restore focus if typing
                if (activeControlName) {
                    const field = filterForm.elements.namedItem(activeControlName);
                    if (field) {
                        field.focus();
                        if (typeof field.setSelectionRange === 'function') {
                            const len = field.value.length;
                            field.setSelectionRange(len, len);
                        }
                    }
                }
            })
            .catch(function (error) {
                console.warn('AJAX filter failed, falling back to form submit:', error);
                if (typeof filterForm.requestSubmit === 'function') {
                    filterForm.requestSubmit();
                } else {
                    filterForm.submit();
                }
            });
        };

        filterForm.querySelectorAll('[data-auto-submit]').forEach(function (control) {
            control.addEventListener('change', function () {
                performAjaxFilter(control.name);
            });
        });

        filterForm.querySelectorAll('[data-auto-search]').forEach(function (control) {
            let searchTimer;
            control.addEventListener('input', function () {
                window.clearTimeout(searchTimer);
                searchTimer = window.setTimeout(function () {
                    performAjaxFilter(control.name);
                }, 400);
            });
        });
    });
});
