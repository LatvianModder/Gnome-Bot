const pasteDataTag = document.getElementById('paste-data');
const pasteTitleTag = document.getElementById('paste-title');
const pasteTextTag = document.getElementById('paste-text');

console.log("Loading " + pasteDataTag.getAttribute('href'))

fetch(pasteDataTag.getAttribute('href')).then(response => {
	pasteTitleTag.innerHTML = response.headers.get('Gnome-Paste-Filename') + ' by ' + response.headers.get('Gnome-Paste-UserName');
	pasteTextTag.innerHTML = "Loading..."
	return response.text();
}).then(text => {
	console.log("Received " + text + " (" + text.length + ")")
	pasteTextTag.innerText = text;
})