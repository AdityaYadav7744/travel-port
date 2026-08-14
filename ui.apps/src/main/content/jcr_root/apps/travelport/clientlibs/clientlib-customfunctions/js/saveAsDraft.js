/**
 * Saves the Adaptive Form as a draft.
 *
 * @name saveAsDraft Save As Draft
 * @function
 * @returns {string} Status message indicating that the draft save was initiated.
 */
function saveAsDraft() {
    return saveFormAsDraft();
}

/**
 * Fetches AEM CSRF token to prevent HTTP 409 (Conflict) errors on POST requests.
 *
 * @private
 * @param {Function} callback Callback with CSRF token string.
 */
function getCsrfToken(callback) {
    try {
        if (window.Granite && window.Granite.HTTP && typeof window.Granite.HTTP.getToken === "function") {
            var token = window.Granite.HTTP.getToken();
            if (token) {
                callback(token);
                return;
            }
        }
    } catch (e) {
        console.warn("[Travelport] Error accessing Granite.HTTP.getToken:", e);
    }

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

/**
 * Saves the form data to the draft service.
 *
 * @private
 * @returns {string} Status message.
 */
function saveFormAsDraft() {
    console.log("[Travelport] saveAsDraft custom function triggered.");

    var formPath = window.location.pathname || "/content/forms/af/loan-form";

    /**
     * Sends the form data to the draft service with CSRF protection.
     *
     * @param {string|Object} formData Form data to save.
     * @returns {void}
     */
    function doSave(formData) {
        var dataString = typeof formData === "string"
            ? formData
            : JSON.stringify(formData);

        getCsrfToken(function (csrfToken) {
            var payload =
                "formPath=" + encodeURIComponent(formPath) +
                "&data=" + encodeURIComponent(dataString);

            if (csrfToken) {
                payload += "&:cq_csrf_token=" + encodeURIComponent(csrfToken);
            }

            var xhr = new XMLHttpRequest();

            xhr.open(
                "POST",
                "/bin/travelport/local-mysql-draft",
                true
            );

            xhr.setRequestHeader(
                "Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8"
            );

            if (csrfToken) {
                xhr.setRequestHeader("CSRF-Token", csrfToken);
            }

            xhr.onload = function () {
                if (xhr.status >= 200 && xhr.status < 300) {
                    try {
                        var response = JSON.parse(xhr.responseText);

                        console.log(
                            "[Travelport] Draft saved successfully:",
                            response
                        );

                        showDraftToast(
                            "Draft Saved! [ID: " +
                            (response.draftId || "N/A") +
                            "]",
                            false
                        );
                    } catch (error) {
                        console.warn(
                            "[Travelport] Draft response is not valid JSON."
                        );

                        showDraftToast(
                            "Draft Saved!",
                            false
                        );
                    }
                } else {
                    console.error(
                        "[Travelport] Draft save failed with status:",
                        xhr.status,
                        xhr.responseText
                    );

                    showDraftToast(
                        "Failed to save draft. Status: " + xhr.status,
                        true
                    );
                }
            };

            xhr.onerror = function () {
                console.error(
                    "[Travelport] Network error while saving draft."
                );

                showDraftToast(
                    "Network error while saving draft.",
                    true
                );
            };

            xhr.send(payload);
        });
    }

    try {
        if (
            window.guideBridge &&
            typeof window.guideBridge.getFormData === "function"
        ) {
            window.guideBridge.getFormData({
                success: function (result) {
                    var formData = result
                        ? (result.data || result)
                        : getDOMFormValues();

                    doSave(formData);
                },

                error: function (error) {
                    console.warn(
                        "[Travelport] GuideBridge getFormData failed. " +
                        "Using DOM fallback.",
                        error
                    );

                    doSave(getDOMFormValues());
                }
            });
        } else {
            console.warn(
                "[Travelport] GuideBridge is not available. " +
                "Using DOM fallback."
            );

            doSave(getDOMFormValues());
        }
    } catch (error) {
        console.error(
            "[Travelport] Error while collecting form data:",
            error
        );

        doSave(getDOMFormValues());
    }

    return "Draft save initiated";
}

/**
 * Collects form field values from the DOM.
 *
 * @private
 * @returns {Object} Form field values.
 */
function getDOMFormValues() {
    var data = {};

    try {
        var inputs = document.querySelectorAll(
            "input, select, textarea"
        );

        for (var i = 0; i < inputs.length; i++) {
            var element = inputs[i];

            if (
                element.name &&
                element.value !== undefined &&
                element.value !== ""
            ) {
                data[element.name] = element.value;
            }
        }
    } catch (error) {
        console.error(
            "[Travelport] Error reading DOM form values:",
            error
        );
    }

    return data;
}

/**
 * Displays a draft status message.
 *
 * @private
 * @param {string} message Message to display.
 * @param {boolean} isError Indicates whether this is an error message.
 * @returns {void}
 */
function showDraftToast(message, isError) {
    var existingToast = document.getElementById(
        "travelport-draft-toast"
    );

    if (existingToast && existingToast.parentNode) {
        existingToast.parentNode.removeChild(existingToast);
    }

    var toast = document.createElement("div");

    toast.id = "travelport-draft-toast";
    toast.innerText = message;

    toast.style.cssText =
        "position:fixed;" +
        "bottom:24px;" +
        "right:24px;" +
        "z-index:999999;" +
        "padding:14px 24px;" +
        "border-radius:8px;" +
        "font-size:15px;" +
        "font-weight:bold;" +
        "color:#fff;" +
        "box-shadow:0 4px 12px rgba(0,0,0,0.25);" +
        "transition:opacity 0.4s ease;" +
        "background-color:" +
        (isError ? "#d9534f" : "#28a745") +
        ";";

    document.body.appendChild(toast);

    setTimeout(function () {
        toast.style.opacity = "0";

        setTimeout(function () {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 400);
    }, 4000);
}

/**
 * Auto-populates form data when draftId query parameter is present in URL.
 */
(function checkAndRestoreDraft() {
    function restoreDraft() {
        var urlParams = new URLSearchParams(window.location.search);
        var draftId = urlParams.get("draftId");
        if (!draftId) return;

        console.log("[Travelport] Attempting to restore draft with ID:", draftId);

        var xhr = new XMLHttpRequest();
        xhr.open("GET", "/bin/travelport/local-mysql-draft?draftId=" + encodeURIComponent(draftId), true);
        xhr.onload = function () {
            if (xhr.status >= 200 && xhr.status < 300) {
                try {
                    var response = JSON.parse(xhr.responseText);
                    if (response && response.status === "success" && response.draft && response.draft.formData) {
                        var rawData = response.draft.formData;
                        var formDataObj = null;

                        if (typeof rawData === "string") {
                            try {
                                formDataObj = JSON.parse(rawData);
                            } catch (e) {
                                formDataObj = rawData;
                            }
                        } else {
                            formDataObj = rawData;
                        }

                        applyDataToForm(rawData, formDataObj);
                        showDraftToast("Draft restored successfully!", false);
                    }
                } catch (e) {
                    console.error("[Travelport] Error parsing draft JSON for restore:", e);
                }
            }
        };
        xhr.send();
    }

    function applyDataToForm(rawData, dataObj) {
        if (!dataObj || typeof dataObj !== "object") return;

        var restoredViaBridge = false;

        // Method 1: Traverse guideBridge Form Model tree
        if (window.guideBridge && typeof window.guideBridge.getFormModel === "function") {
            try {
                var model = window.guideBridge.getFormModel();
                if (model && typeof model.visit === "function") {
                    model.visit(function (node) {
                        if (node && node.name && dataObj[node.name] !== undefined) {
                            var val = dataObj[node.name];
                            if (val !== null && val !== undefined && !node.name.startsWith(":")) {
                                node.value = val;
                                restoredViaBridge = true;
                            }
                        }
                    });
                }
            } catch (e) {
                console.warn("[Travelport] Model visit failed:", e);
            }
        }

        // Method 2: guideBridge.setData / importData
        if (!restoredViaBridge && window.guideBridge) {
            if (typeof window.guideBridge.setData === "function") {
                try {
                    window.guideBridge.setData({ data: dataObj });
                    restoredViaBridge = true;
                } catch (e) {}
            } else if (typeof window.guideBridge.importData === "function") {
                try {
                    window.guideBridge.importData({
                        data: typeof rawData === "string" ? rawData : JSON.stringify(rawData),
                        mimeType: "application/json"
                    });
                    restoredViaBridge = true;
                } catch (e) {}
            }
        }

        // Method 3: DOM Fallback ONLY if guideBridge model restoration didn't happen
        if (!restoredViaBridge) {
            populateDOMInputs(dataObj);
        }
    }

    function populateDOMInputs(data) {
        if (!data || typeof data !== "object") return;
        for (var key in data) {
            if (!data.hasOwnProperty(key) || key.startsWith(":")) continue;
            var val = data[key];
            if (val === null || val === undefined) continue;

            try {
                var elements = document.querySelectorAll('[name="' + key + '"], [id*="' + key + '"]');
                for (var i = 0; i < elements.length; i++) {
                    var el = elements[i];
                    if (el.tagName === "INPUT" || el.tagName === "SELECT" || el.tagName === "TEXTAREA") {
                        el.value = val;
                    }
                }
            } catch (e) {}
        }
    }

    function initRestore() {
        if (window.guideBridge && typeof window.guideBridge.connect === "function") {
            window.guideBridge.connect(function () {
                restoreDraft();
            });
        } else {
            window.addEventListener("bridgeInitializeStart", function () {
                if (window.guideBridge && typeof window.guideBridge.connect === "function") {
                    window.guideBridge.connect(function () {
                        restoreDraft();
                    });
                }
            });
            document.addEventListener("DOMContentLoaded", function () {
                setTimeout(restoreDraft, 800);
            });
        }
    }

    initRestore();
})();