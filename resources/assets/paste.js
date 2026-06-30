const pasteTitleTag = document.getElementById('paste-title');
const pasteTextTag = document.getElementById('paste-text');
const pasteFilename = pasteTextTag.dataset.pasteFilename;
const pasteUrl = pasteTextTag.dataset.pasteUrl;
const pasteContentType = pasteTextTag.dataset.pasteContentType;
const pasteZipPath = pasteTextTag.dataset.zipPath;

console.log(`Loading ${pasteFilename} @ ${pasteUrl}`);

const javaAndJsKeywords = [
	/* Common */
	"throw", "try", "catch", "return", "break", "continue", "if", "while", "for", "do", "else", "switch", "case", "true", "false", "new", "var", "this",
	/* Java */
	"public", "private", "protected", "abstract", "static", "final", "transient", "volatile", "throws", "class", "interface", "@interface",
	"record", "enum", "extends", "implements", "super", "import", "package", "void", "byte", "short", "int", "long", "float", "double", "char", "String", "Number",
	/* JS */
	"let", "const", "console", "function", "number",
	/* TS */
	"module", "export", "readonly", "constructor", "type", "any"
].join("|");

const javaAndJsKeywordsPattern = new RegExp("(([\"'`]).*?\\2)|(\\d+(?:\\.\\d+)?)|(\\b(?:" + javaAndJsKeywords + ")\\b)|([;=\\-+*/%&|^~!:?.,])|([{(<\\[])|([})>\\]])");
const stackAtPattern = new RegExp(/([\s\t]+at )(?:([\w\-./$@\s*+]+)\/)?([\w.$@]+)\.([\w/$]+)\.(<init>|<clinit>|[\w$]+)\((Unknown Source|\.dynamic|Native Method|[\w.$]+:\d+)\)(?: ~?\[.*:.*])?(?: \{.*})?/)

const mcUUIDPattern = new RegExp(/--uuid,\s?(\w{32})/);
const mcUsernamePattern = new RegExp(/--username,\s?(\w+)/);

if (pasteContentType.startsWith("image/")) {
	const img = document.createElement("img");
	img.src = pasteUrl;
	pasteTextTag.appendChild(img);
} else if (pasteContentType.startsWith("video/")) {
	const video = document.createElement("video");
	video.src = pasteUrl;
	pasteTextTag.appendChild(video);
} else {
	fetch(pasteUrl).then(response => response.text()).then(text => {
		pasteTextTag.innerHTML = ""
		// console.log("Received " + text + " (" + text.length + ")")

		const lines = text.split("\n");
		const padding = lines.length.toFixed().length;
		const isLanguage = pasteFilename.endsWith(".java") || pasteFilename.endsWith(".js") || pasteFilename.endsWith(".ts") || pasteFilename.endsWith(".zs") || pasteFilename.endsWith(".json");
		let mcUUID = "";
		let mcUsername = "";

		lines.forEach((line, index) => {
			const lineName = "L" + index.toFixed()
			const lineTag = document.createElement("span");
			lineTag.id = lineName;

			if (!isLanguage) {
				if (line.includes("ERR")) {
					lineTag.classList.add("c-e");
				} else if (line.includes("WARN")) {
					lineTag.classList.add("c-w");
				} else if (line.includes("DEBUG") || line.includes("TRACE")) {
					lineTag.classList.add("c-d");
				} else if (line.includes("Error:") || line.includes("Exception:") || line.includes("Caused by:") || line.includes("Stacktrace:")) {
					lineTag.classList.add("c-e");
				}
			}

			const lineNumTag = document.createElement("a");
			lineNumTag.href = "#" + lineName;
			lineNumTag.innerText = index.toFixed().padStart(padding, '0');
			lineTag.appendChild(lineNumTag);

			const spaceTag = document.createElement("a");
			spaceTag.innerText = "    ";
			lineTag.appendChild(spaceTag);

			lineTag.append(line);

			pasteTextTag.appendChild(lineTag);
		})
	})
}