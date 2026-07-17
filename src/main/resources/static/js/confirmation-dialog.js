(function () {
    "use strict";

    var activeForm = null;
    var activeSubmitter = null;
    var dialog = null;

    function createDialog() {
        if (dialog) {
            return dialog;
        }

        dialog = document.createElement("dialog");
        dialog.className = "app-confirm-dialog";
        dialog.setAttribute("aria-labelledby", "appConfirmDialogTitle");
        dialog.setAttribute("aria-describedby", "appConfirmDialogMessage");
        dialog.innerHTML =
            '<div class="app-confirm-dialog__shell">' +
                '<header class="app-confirm-dialog__header">' +
                    '<div class="app-confirm-dialog__heading">' +
                        '<h2 class="app-confirm-dialog__title" id="appConfirmDialogTitle">Xác nhận thao tác</h2>' +
                        '<p class="app-confirm-dialog__subtitle">Kiểm tra thông tin trước khi xác nhận.</p>' +
                    '</div>' +
                '</header>' +
                '<div class="app-confirm-dialog__body">' +
                    '<div class="app-confirm-dialog__subject" hidden>' +
                        '<span class="app-confirm-dialog__subject-label">Thông tin</span>' +
                        '<strong class="app-confirm-dialog__subject-value"></strong>' +
                    '</div>' +
                    '<p class="app-confirm-dialog__message" id="appConfirmDialogMessage"></p>' +
                '</div>' +
                '<footer class="app-confirm-dialog__footer">' +
                    '<button type="button" class="app-confirm-dialog__button app-confirm-dialog__cancel">Quay lại</button>' +
                    '<button type="button" class="app-confirm-dialog__button app-confirm-dialog__confirm">Xác nhận</button>' +
                '</footer>' +
            '</div>';
        document.body.appendChild(dialog);

        dialog.querySelector(".app-confirm-dialog__cancel").addEventListener("click", closeDialog);
        dialog.querySelector(".app-confirm-dialog__confirm").addEventListener("click", confirmSubmission);
        dialog.addEventListener("click", function (event) {
            if (event.target === dialog) {
                closeDialog();
            }
        });
        dialog.addEventListener("close", function () {
            activeForm = null;
            activeSubmitter = null;
        });

        return dialog;
    }

    function closeDialog() {
        if (dialog && dialog.open) {
            dialog.close();
        }
    }

    function confirmSubmission() {
        if (!activeForm) {
            closeDialog();
            return;
        }

        var form = activeForm;
        var submitter = activeSubmitter;
        form.dataset.confirmBypass = "true";
        closeDialog();

        if (typeof form.requestSubmit === "function") {
            form.requestSubmit(submitter || undefined);
        } else {
            form.submit();
        }
    }

    document.addEventListener("submit", function (event) {
        var form = event.target;
        if (!form.classList.contains("app-confirm-form")) {
            return;
        }

        if (form.dataset.confirmBypass === "true") {
            delete form.dataset.confirmBypass;
            return;
        }

        event.preventDefault();
        activeForm = form;
        activeSubmitter = event.submitter || null;

        var currentDialog = createDialog();
        var title = form.dataset.confirmTitle || "Xác nhận thao tác";
        var message = form.dataset.confirmMessage || "Bạn có chắc chắn muốn thực hiện thao tác này?";
        var subject = form.dataset.confirmSubject || "";
        var subjectLabel = form.dataset.confirmSubjectLabel || "Thông tin";
        var actionLabel = form.dataset.confirmAction || "Xác nhận";
        var isDanger = form.dataset.confirmTone === "danger";
        var subjectElement = currentDialog.querySelector(".app-confirm-dialog__subject");

        currentDialog.querySelector(".app-confirm-dialog__title").textContent = title;
        currentDialog.querySelector(".app-confirm-dialog__message").textContent = message;
        currentDialog.querySelector(".app-confirm-dialog__subject-label").textContent = subjectLabel;
        currentDialog.querySelector(".app-confirm-dialog__subject-value").textContent = subject;
        currentDialog.querySelector(".app-confirm-dialog__confirm").textContent = actionLabel;
        subjectElement.hidden = !subject;
        currentDialog.classList.toggle("is-danger", isDanger);
        currentDialog.showModal();
    });
})();
