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

const javaAndJsKeywordsPattern = new RegExp("(([\"'`]).*?\\2)|(\\d+(?:\\.\\d+)?)|(\\b(?:" + javaAndJsKeywords + ")\\b)|([;=\\-+*/%&|^~!:?.,])|([{(<\\[])|([})>\\]])", "g");
const stackAtPattern = /([\s\t]+at )(?:([\w\-./$@\s*+]+)\/)?([\w.$@]+)\.([\w/$]+)\.(<init>|<clinit>|[\w$]+)\((Unknown Source|\.dynamic|Native Method|[\w.$]+:\d+)\)(?: ~?\[.*:.*])?(?: \{.*})?/g;

const mcUUIDPattern = new RegExp(/--uuid,\s?(\w{32})/);
const mcUsernamePattern = new RegExp(/--username,\s?(\w+)/);

function appendText(parent, text) {
	if (text.length > 0) {
		parent.appendChild(document.createTextNode(text));
	}
}

function appendSpan(parent, className, text) {
	const span = document.createElement("span");
	span.classList.add(className);
	appendText(span, text);
	parent.appendChild(span);
	return span;
}

function appendMatchedText(parent, line, pattern, appendMatch) {
	let lastIndex = 0;
	pattern.lastIndex = 0;

	for (let match = pattern.exec(line); match !== null; match = pattern.exec(line)) {
		appendText(parent, line.substring(lastIndex, match.index));
		appendMatch(match);
		lastIndex = pattern.lastIndex;
	}

	appendText(parent, line.substring(lastIndex));
}

function readPasteLines(text) {
	const lines = text.split(/\r\n|\n|\r/);

	if (lines.length > 0 && lines[lines.length - 1] === "") {
		lines.pop();
	}

	return lines;
}

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

		const lines = readPasteLines(text);
		const padding = lines.length.toFixed().length;
		const isLanguage = pasteFilename.endsWith(".java") || pasteFilename.endsWith(".js") || pasteFilename.endsWith(".ts") || pasteFilename.endsWith(".zs") || pasteFilename.endsWith(".json");
		console.log(`Is Language: ${isLanguage}`);
		let mcUUID = "";
		let mcUsername = "";

		lines.forEach((line, index) => {
			if (mcUUID.length === 0) {
				const match = mcUUIDPattern.exec(line);

				if (match !== null) {
					mcUUID = match[1];
				}
			}

			if (mcUsername.length === 0) {
				const match = mcUsernamePattern.exec(line);

				if (match !== null) {
					mcUsername = match[1];
				}
			}

			const lineNumber = index + 1;
			const lineName = "L" + lineNumber.toFixed()
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
			lineNumTag.innerText = lineNumber.toFixed().padStart(padding, '0');
			lineTag.appendChild(lineNumTag);

			const spaceTag = document.createElement("a");
			// spaceTag.href = "";
			spaceTag.innerText = "    ";
			lineTag.appendChild(spaceTag);

			if (isLanguage) {
				appendMatchedText(lineTag, line, javaAndJsKeywordsPattern, match => {
					const string = match[1];
					const number = match[3];
					const keyword = match[4];
					const symbol = match[5];
					const bracketOpen = match[6];
					const bracketClose = match[7];

					if (string !== undefined) {
						appendSpan(lineTag, "f-g", string);
					} else if (number !== undefined) {
						appendSpan(lineTag, "f-o", number);
					} else if (keyword !== undefined) {
						appendSpan(lineTag, "f-m", keyword);
					} else if (symbol !== undefined) {
						appendSpan(lineTag, "f-b", symbol);
					} else if (bracketOpen !== undefined) {
						appendSpan(lineTag, "f-b", bracketOpen);
					} else if (bracketClose !== undefined) {
						appendSpan(lineTag, "f-b", bracketClose);
					}
				});
			} else {
				appendMatchedText(lineTag, line, stackAtPattern, match => {
					const at = match[1];
					const moduleName = match[2];
					const packagePath = match[3];
					const className = match[4];
					const methodName = match[5];
					const source = match[6];

					appendText(lineTag, at);

					const packageSpan = appendSpan(lineTag, "f-o", packagePath);

					if (moduleName !== undefined) {
						packageSpan.title = moduleName;
					}

					appendText(lineTag, ".");
					appendSpan(lineTag, "f-y", className);
					appendText(lineTag, ".");
					appendSpan(lineTag, "f-b", methodName);
					appendText(lineTag, ":");

					const sourceSet = new Set(className.split("$"));
					const sourceParts = source.split(":", 2);
					const lineSpan = appendSpan(lineTag, "f-p", "");

					if (sourceParts[0] === "Native Method") {
						appendText(lineSpan, "native");
					} else if (sourceParts[0] === "Unknown Source") {
						appendText(lineSpan, "unknown");
					} else if (sourceParts[0] === ".dynamic") {
						appendText(lineSpan, "dynamic");
					} else if (sourceParts[0] === "SourceFile") {
						appendText(lineSpan, "SourceFile");
					} else if (sourceParts.length === 2 && sourceSet.has(sourceParts[0].replace(".java", ""))) {
						appendText(lineSpan, "L" + sourceParts[1]);
					} else {
						appendText(lineSpan, source.replace(".java", ""));
					}

					lineSpan.title = line;
				});
			}

			pasteTextTag.appendChild(lineTag);
		})

		if (mcUUID.length > 0 || mcUsername.length > 0) {
			const mc = mcUUID.length === 0 ? mcUsername : mcUUID;
			const link = document.createElement("a");
			link.href = "https://mcuuid.net/?q=" + mc;
			link.title = mcUsername;

			appendText(link, " ");

			const img = document.createElement("img");
			img.src = "https://crafthead.net/avatar/" + mc + "/48";
			img.classList.add("inline-img");
			link.appendChild(img);

			appendText(link, " [Minecraft Profile]");
			pasteTitleTag.appendChild(link);
		}
	})
}