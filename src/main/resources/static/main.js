setTimeout(function () {
    console.log("wait...")
}, 2000);

function displayResponseFromJava(message) {
    document.getElementById('response').innerHTML = JSON.stringify(message, null, 2);
}

document.getElementById('sendButton').addEventListener('click', function () {
    var el = document.getElementById('textInput');
    window.JSJavaBridge.callJava(el.value);
    el.value = JSON.stringify(JSON.parse(el.value), null, 2);
});
