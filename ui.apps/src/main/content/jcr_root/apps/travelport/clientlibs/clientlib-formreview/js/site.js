window.guideBridge.connect(function () {

    console.log("GuideBridge Connected");

    var form = window.guideBridge.resolveNode("$form");

    console.log("Form Object:", form);

});