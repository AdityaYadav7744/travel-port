(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        var container = document.getElementById("tp-drafts-wrapper");
        if (!container) return;

        var loader = document.getElementById("tp-drafts-loader");
        var errorAlert = document.getElementById("tp-drafts-error");
        var errorMessage = document.getElementById("tp-error-message");
        var emptyState = document.getElementById("tp-drafts-empty-state");
        var tableContainer = document.getElementById("tp-drafts-table-container");
        var tableBody = document.getElementById("tp-drafts-table-body");
        var badgeCount = document.getElementById("tp-drafts-badge");
        var refreshBtn = document.getElementById("tp-refresh-drafts-btn");

        // Modal Elements
        var modal = document.getElementById("tp-draft-modal");
        var jsonCode = document.getElementById("tp-modal-json");
        var modalCloseBtn = document.getElementById("tp-modal-close-btn");
        var modalDoneBtn = document.getElementById("tp-modal-done-btn");
        var modalCopyBtn = document.getElementById("tp-modal-copy-btn");

        function getCsrfToken(callback) {
            try {
                if (window.Granite && window.Granite.HTTP && typeof window.Granite.HTTP.getToken === "function") {
                    var token = window.Granite.HTTP.getToken();
                    if (token) {
                        callback(token);
                        return;
                    }
                }
            } catch (e) {}

            var xhr = new XMLHttpRequest();
            xhr.open("GET", "/libs/granite/csrf/token.json", true);
            xhr.onload = function () {
                if (xhr.status === 200) {
                    try {
                        var json = JSON.parse(xhr.responseText);
                        callback(json.token || "");
                    } catch (e) {
                        callback("");
                    }
                } else {
                    callback("");
                }
            };
            xhr.onerror = function () {
                callback("");
            };
            xhr.send();
        }

        function loadDrafts() {
            loader.style.display = "block";
            errorAlert.style.display = "none";
            emptyState.style.display = "none";
            tableContainer.style.display = "none";

            var xhr = new XMLHttpRequest();
            xhr.open("GET", "/bin/travelport/local-mysql-draft", true);
            xhr.onload = function () {
                loader.style.display = "none";
                if (xhr.status >= 200 && xhr.status < 300) {
                    try {
                        var response = JSON.parse(xhr.responseText);
                        var drafts = response.drafts || [];
                        renderDrafts(drafts);
                    } catch (e) {
                        showError("Invalid JSON response from draft service.");
                    }
                } else {
                    showError("Failed to fetch drafts (Status " + xhr.status + ").");
                }
            };
            xhr.onerror = function () {
                loader.style.display = "none";
                showError("Network error while connecting to draft service.");
            };
            xhr.send();
        }

        function showError(msg) {
            errorMessage.innerText = msg;
            errorAlert.style.display = "flex";
            badgeCount.innerText = "0 Drafts";
        }

        function renderDrafts(drafts) {
            badgeCount.innerText = drafts.length + (drafts.length === 1 ? " Draft" : " Drafts");

            if (drafts.length === 0) {
                emptyState.style.display = "block";
                return;
            }

            tableBody.innerHTML = "";

            drafts.forEach(function (draft) {
                var tr = document.createElement("tr");

                var shortId = (draft.draftId || "N/A");
                if (shortId.length > 18) {
                    shortId = shortId.substring(0, 18) + "...";
                }

                var title = draft.title || "Form Draft";
                var formPath = draft.formPath || "";
                var updatedAt = formatDate(draft.updatedAt);

                tr.innerHTML =
                    '<td>' +
                        '<strong>' + escapeHtml(title) + '</strong><br/>' +
                        '<small style="color: #718096;">' + escapeHtml(formPath) + '</small>' +
                    '</td>' +
                    '<td><span class="tp-draft-id-code" title="' + escapeHtml(draft.draftId) + '">' + escapeHtml(shortId) + '</span></td>' +
                    '<td>' + escapeHtml(updatedAt) + '</td>' +
                    '<td class="tp-action-cell">' +
                        '<button type="button" class="tp-btn tp-btn-secondary tp-btn-sm tp-view-json-btn" data-json="' + escapeHtml(draft.formData) + '">👁️ View Data</button>' +
                        '<a href="' + escapeHtml(buildResumeUrl(formPath, draft.draftId)) + '" target="_blank" class="tp-btn tp-btn-primary tp-btn-sm">🚀 Resume</a>' +
                        '<button type="button" class="tp-btn tp-btn-danger tp-btn-sm tp-delete-btn" data-id="' + escapeHtml(draft.draftId) + '">🗑️ Delete</button>' +
                    '</td>';

                tableBody.appendChild(tr);
            });

            tableContainer.style.display = "block";
            bindRowEvents();
        }

        function buildResumeUrl(formPath, draftId) {
            if (!formPath) formPath = "/content/forms/af/job-application-form.html";
            var url = formPath;
            if (!url.endsWith(".html") && !url.includes("?")) {
                url += ".html";
            }
            url += (url.includes("?") ? "&" : "?") + "draftId=" + encodeURIComponent(draftId);
            return url;
        }

        function formatDate(dateStr) {
            if (!dateStr) return "Just now";
            try {
                var d = new Date(dateStr);
                if (isNaN(d.getTime())) return dateStr;
                return d.toLocaleDateString() + " " + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            } catch (e) {
                return dateStr;
            }
        }

        function escapeHtml(str) {
            if (!str) return "";
            return String(str)
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#039;");
        }

        function bindRowEvents() {
            var viewBtns = tableBody.querySelectorAll(".tp-view-json-btn");
            viewBtns.forEach(function (btn) {
                btn.addEventListener("click", function () {
                    var rawJson = btn.getAttribute("data-json");
                    try {
                        var parsed = JSON.parse(rawJson);
                        jsonCode.innerText = JSON.stringify(parsed, null, 2);
                    } catch (e) {
                        jsonCode.innerText = rawJson || "No data payload";
                    }
                    modal.style.display = "flex";
                });
            });

            var deleteBtns = tableBody.querySelectorAll(".tp-delete-btn");
            deleteBtns.forEach(function (btn) {
                btn.addEventListener("click", function () {
                    var draftId = btn.getAttribute("data-id");
                    if (confirm("Are you sure you want to delete this draft?")) {
                        deleteDraft(draftId);
                    }
                });
            });
        }

        function deleteDraft(draftId) {
            getCsrfToken(function (token) {
                var xhr = new XMLHttpRequest();
                var url = "/bin/travelport/local-mysql-draft";
                var payload = "action=delete&draftId=" + encodeURIComponent(draftId);

                if (token) {
                    payload += "&:cq_csrf_token=" + encodeURIComponent(token);
                }

                xhr.open("POST", url, true);
                xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                if (token) {
                    xhr.setRequestHeader("CSRF-Token", token);
                }

                xhr.onload = function () {
                    if (xhr.status >= 200 && xhr.status < 300) {
                        loadDrafts();
                    } else {
                        alert("Failed to delete draft. Status: " + xhr.status);
                    }
                };
                xhr.onerror = function () {
                    alert("Network error while deleting draft.");
                };
                xhr.send(payload);
            });
        }

        function closeModal() {
            modal.style.display = "none";
        }

        if (modalCloseBtn) modalCloseBtn.addEventListener("click", closeModal);
        if (modalDoneBtn) modalDoneBtn.addEventListener("click", closeModal);
        if (refreshBtn) refreshBtn.addEventListener("click", loadDrafts);

        if (modalCopyBtn) {
            modalCopyBtn.addEventListener("click", function () {
                var text = jsonCode.innerText;
                navigator.clipboard.writeText(text).then(function () {
                    modalCopyBtn.innerText = "Copied! ✓";
                    setTimeout(function () {
                        modalCopyBtn.innerText = "Copy JSON";
                    }, 2000);
                });
            });
        }

        // Initial Load
        loadDrafts();
    });
})();
