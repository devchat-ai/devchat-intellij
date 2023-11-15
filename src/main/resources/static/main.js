setTimeout(function () {
    console.log("wait...")
}, 2000);

function displayResponseFromJava(message) {
    document.getElementById('response').innerHTML = JSON.stringify(message);
}

document.getElementById('sendButton').addEventListener('click', function () {
    var textInput = document.getElementById('textInput').value;
    window.JSJavaBridge.callJava(textInput);
});
