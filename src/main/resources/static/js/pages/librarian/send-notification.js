(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        const form = document.getElementById("notificationForm");
        if (!form) return;

        const all = document.getElementById("recipientAll");
        const selected = document.getElementById("recipientSelected");
        const memberSection = document.getElementById("memberListBox");
        const selectedBox = document.getElementById("selectedMembers");
        const resultsBox = document.getElementById("memberSearchResults");
        const search = document.getElementById("memberSearchInput");
        const searchStatus = document.getElementById("memberSearchStatus");
        const selectedCount = document.getElementById("selectedMemberCount");
        const recipientGroup = document.getElementById("recipientTypeGroup");
        const recipientError = document.getElementById("recipientTypeError");
        const memberError = document.getElementById("memberIdsError");
        const submit = document.getElementById("notificationSubmit");
        const title = document.getElementById("title");
        const titleError = document.getElementById("titleValidationMessage");
        const content = document.getElementById("content");
        const contentError = document.getElementById("contentValidationMessage");
        const type = document.getElementById("notificationType");
        const typeError = document.getElementById("notificationTypeError");
        const confirmDialog = document.getElementById("notificationConfirmDialog");
        const confirmRecipients = document.getElementById("notificationConfirmRecipients");
        const confirmTitle = document.getElementById("notificationConfirmMessageTitle");
        const confirmType = document.getElementById("notificationConfirmType");
        const confirmContent = document.getElementById("notificationConfirmContent");
        const confirmCancel = document.getElementById("notificationConfirmCancel");
        const confirmSubmit = document.getElementById("notificationConfirmSubmit");
        const minSearch = Number(search.dataset.minLength);
        const maximum = Number(search.dataset.maximum);
        let searchTimer;
        let searchController;
        let submitting = false;
        let confirmationAccepted = false;

        function checkboxes() {
            return Array.from(selectedBox.querySelectorAll("input[name='memberIds']"));
        }

        function selectedIds() {
            return checkboxes().filter(function (box) { return box.checked; })
                .map(function (box) { return box.value; });
        }

        function refreshSelection() {
            selectedCount.textContent = String(selectedIds().length);
            checkboxes().forEach(function (box) { box.disabled = !selected.checked; });
        }

        function toggleRecipients() {
            memberSection.classList.toggle("is-hidden", !selected.checked);
            refreshSelection();
        }

        function clearFieldValidation(field, errorElement) {
            field.classList.remove("is-invalid", "is-valid");
            field.removeAttribute("aria-invalid");
            if (errorElement) errorElement.textContent = "";
        }

        function setFieldValidation(field, errorElement, message) {
            field.classList.remove("is-invalid", "is-valid");
            field.classList.add(message ? "is-invalid" : "is-valid");
            field.setAttribute("aria-invalid", String(Boolean(message)));
            if (errorElement) errorElement.textContent = message || "";
        }

        function containsLetter(value) {
            return /\p{L}/u.test(value);
        }

        function comparableText(value) {
            return value.trim().replace(/\s+/g, " ").toLocaleLowerCase();
        }

        function validateForm() {
            let firstInvalid = null;
            const recipientMessage = all.checked || selected.checked
                ? ""
                : form.dataset.recipientRequired;
            setFieldValidation(recipientGroup, recipientError, recipientMessage);
            if (recipientMessage) firstInvalid = all;

            const typeMessage = type.value ? "" : form.dataset.typeRequired;
            setFieldValidation(type, typeError, typeMessage);
            if (!firstInvalid && typeMessage) firstInvalid = type;

            let titleMessage = "";
            if (title.value === "") titleMessage = form.dataset.titleRequired;
            else if (title.value.length < Number(form.dataset.titleMin)) titleMessage = form.dataset.titleMinimum;
            else if (title.value.length > Number(form.dataset.titleMax)) titleMessage = form.dataset.titleMaximum;
            else if (!containsLetter(title.value)) titleMessage = form.dataset.titleLetters;
            setFieldValidation(title, titleError, titleMessage);
            if (!firstInvalid && titleMessage) firstInvalid = title;

            let contentMessage = "";
            if (content.value === "") contentMessage = form.dataset.contentRequired;
            else if (content.value.length < Number(form.dataset.contentMin)) contentMessage = form.dataset.contentMinimum;
            else if (content.value.length > Number(form.dataset.contentMax)) contentMessage = form.dataset.contentMaximum;
            else if (!containsLetter(content.value)) contentMessage = form.dataset.contentLetters;
            else if (title.value !== "" && comparableText(content.value) === comparableText(title.value)) {
                contentMessage = form.dataset.contentDifferent;
            }
            setFieldValidation(content, contentError, contentMessage);
            if (!firstInvalid && contentMessage) firstInvalid = content;

            let memberMessage = "";
            if (selected.checked && selectedIds().length === 0) {
                memberMessage = form.dataset.memberRequired;
            } else if (selected.checked && selectedIds().length > maximum) {
                memberMessage = form.dataset.memberMaximum;
            }
            setFieldValidation(selectedBox, memberError, memberMessage);
            if (!firstInvalid && memberMessage) firstInvalid = search;

            if (firstInvalid) firstInvalid.focus();
            return firstInvalid == null;
        }

        function setSearchStatus(message) {
            searchStatus.textContent = message || "";
        }

        function clearResults() {
            resultsBox.replaceChildren();
            resultsBox.hidden = true;
        }

        function detailText(member) {
            return [member.memberCode, member.email, member.phone].filter(Boolean).join(" | ");
        }

        function addRecipient(member) {
            let item = selectedBox.querySelector("[data-member-id='" + member.memberId + "']");
            if (item) {
                item.querySelector("input").checked = true;
                refreshSelection();
                return;
            }
            if (selectedIds().length >= maximum) {
                if (memberError) memberError.textContent = form.dataset.memberMaximum;
                return;
            }
            item = document.createElement("div");
            item.className = "notification-member-item member-item";
            item.dataset.memberId = String(member.memberId);
            const box = document.createElement("input");
            box.className = "form-check-input member-checkbox";
            box.type = "checkbox";
            box.name = "memberIds";
            box.value = String(member.memberId);
            box.id = "notificationMember-" + member.memberId;
            box.checked = true;
            const label = document.createElement("label");
            label.className = "form-check-label";
            label.htmlFor = box.id;
            const name = document.createElement("strong");
            name.textContent = member.fullName || member.memberCode;
            const detail = document.createElement("small");
            detail.textContent = detailText(member);
            label.append(name, detail);
            item.append(box, label);
            selectedBox.append(item);
            refreshSelection();
            renderSearchResults([]);
        }

        function renderSearchResults(members) {
            clearResults();
            if (!members.length) {
                setSearchStatus(form.dataset.searchEmpty);
                return;
            }
            const chosen = new Set(selectedIds());
            members.forEach(function (member) {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "notification-member-item notification-search-result";
                button.disabled = chosen.has(String(member.memberId));
                const name = document.createElement("strong");
                name.textContent = member.fullName || member.memberCode;
                const detail = document.createElement("small");
                detail.textContent = detailText(member);
                button.append(name, detail);
                button.addEventListener("click", function () { addRecipient(member); });
                resultsBox.append(button);
            });
            resultsBox.hidden = false;
            setSearchStatus("");
        }

        async function searchRecipients() {
            const query = search.value.trim();
            if (query.length < minSearch) {
                clearResults();
                setSearchStatus(form.dataset.searchMinimum);
                return;
            }
            if (searchController) searchController.abort();
            searchController = new AbortController();
            setSearchStatus(form.dataset.searching);
            try {
                const url = new URL(search.dataset.searchUrl, window.location.origin);
                url.searchParams.set("query", query);
                const response = await fetch(url, {signal: searchController.signal, headers: {"Accept": "application/json"}});
                if (!response.ok) throw new Error("search_failed");
                const page = await response.json();
                renderSearchResults(Array.isArray(page.content) ? page.content : []);
            } catch (error) {
                if (error.name !== "AbortError") {
                    clearResults();
                    setSearchStatus(form.dataset.searchError);
                }
            }
        }

        function updateCounter(input, id, maximum) {
            const counter = document.getElementById(id);
            const length = input.value.length;
            counter.textContent = length + "/" + maximum;
            counter.classList.toggle("is-near-limit", length >= Math.floor(maximum * 0.9) && length < maximum);
            counter.classList.toggle("is-limit-reached", length >= maximum);
        }

        function openConfirmation() {
            const count = selected.checked ? selectedIds().length : Number(form.dataset.activeCount);
            const recipientTemplate = selected.checked ? form.dataset.confirmSelected : form.dataset.confirmAll;
            confirmRecipients.textContent = recipientTemplate.replace("{0}", String(count));
            confirmTitle.textContent = title.value;
            confirmType.textContent = type.options[type.selectedIndex] ? type.options[type.selectedIndex].text : "";
            confirmContent.textContent = content.value;
            confirmDialog.showModal();
            confirmSubmit.focus();
        }

        confirmCancel.addEventListener("click", function () {
            confirmDialog.close();
        });
        confirmSubmit.addEventListener("click", function () {
            confirmationAccepted = true;
            confirmDialog.close();
            form.requestSubmit();
        });

        [all, selected].forEach(function (radio) {
            radio.addEventListener("change", function () {
                clearFieldValidation(recipientGroup, recipientError);
                if (all.checked) clearFieldValidation(selectedBox, memberError);
                toggleRecipients();
            });
        });
        selectedBox.addEventListener("change", function () {
            clearFieldValidation(selectedBox, memberError);
            refreshSelection();
        });
        type.addEventListener("change", function () {
            clearFieldValidation(type, typeError);
        });
        search.addEventListener("input", function () {
            window.clearTimeout(searchTimer);
            searchTimer = window.setTimeout(searchRecipients, 300);
        });
        title.addEventListener("input", function () {
            clearFieldValidation(title, titleError);
            updateCounter(title, "titleCharacterCount", Number(form.dataset.titleMax));
        });
        content.addEventListener("input", function () {
            clearFieldValidation(content, contentError);
            updateCounter(content, "contentCharacterCount", Number(form.dataset.contentMax));
        });

        form.addEventListener("submit", function (event) {
            if (submitting) {
                event.preventDefault();
                return;
            }
            title.value = title.value.trim().replace(/\s+/g, " ");
            content.value = content.value.trim().replace(/(?:\r?\n\s*){3,}/g, "\n\n");
            if (!validateForm()) {
                event.preventDefault();
                return;
            }
            if (!confirmationAccepted) {
                event.preventDefault();
                openConfirmation();
                return;
            }
            confirmationAccepted = false;
            submitting = true;
            submit.disabled = true;
            const label = submit.querySelector(".submit-label");
            if (label) label.textContent = form.dataset.processing;
        });

        toggleRecipients();
        refreshSelection();
        updateCounter(title, "titleCharacterCount", Number(form.dataset.titleMax));
        updateCounter(content, "contentCharacterCount", Number(form.dataset.contentMax));
    });
}());
