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
        const memberError = document.getElementById("memberIdsError");
        const submit = document.getElementById("notificationSubmit");
        const title = document.getElementById("title");
        const content = document.getElementById("content");
        const type = document.getElementById("notificationType");
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
            if (memberError) memberError.textContent = "";
        }

        function toggleRecipients() {
            memberSection.classList.toggle("is-hidden", !selected.checked);
            refreshSelection();
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
            radio.addEventListener("change", toggleRecipients);
        });
        selectedBox.addEventListener("change", refreshSelection);
        search.addEventListener("input", function () {
            window.clearTimeout(searchTimer);
            searchTimer = window.setTimeout(searchRecipients, 300);
        });
        title.addEventListener("input", function () {
            updateCounter(title, "titleCharacterCount", Number(form.dataset.titleMax));
        });
        content.addEventListener("input", function () {
            updateCounter(content, "contentCharacterCount", Number(form.dataset.contentMax));
        });

        form.addEventListener("submit", function (event) {
            if (submitting) {
                event.preventDefault();
                return;
            }
            title.value = title.value.trim().replace(/\s+/g, " ");
            content.value = content.value.trim().replace(/(?:\r?\n\s*){3,}/g, "\n\n");
            if (selected.checked && selectedIds().length === 0) {
                event.preventDefault();
                memberError.textContent = form.dataset.memberRequired;
                search.focus();
                return;
            }
            if (!form.checkValidity()) {
                event.preventDefault();
                form.reportValidity();
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
