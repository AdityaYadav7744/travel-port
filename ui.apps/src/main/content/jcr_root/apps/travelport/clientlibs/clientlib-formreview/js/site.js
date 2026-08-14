(function () {
    function initGuideBridge() {
        if (window.guideBridge && typeof window.guideBridge.connect === "function") {
            window.guideBridge.connect(function () {
                console.log("[Travelport] GuideBridge Connected");
                try {
                    var form = window.guideBridge.resolveNode("$form");
                    console.log("[Travelport] Form Object:", form);
                } catch (e) {
                    console.warn("[Travelport] Error resolving form object:", e);
                }
            });
        }
    }

    if (window.guideBridge && typeof window.guideBridge.connect === "function") {
        initGuideBridge();
    } else {
        window.addEventListener("bridgeInitializeStart", function (ev) {
            initGuideBridge();
        });
    }
})();