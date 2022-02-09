package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.Paste;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.server.html.RootTag;
import dev.gnomebot.app.server.html.Tag;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PasteHandlers {
	public static final Pattern AT_PATTERN = Pattern.compile("([ \\t]+at )([\\w./$@]+)\\.([\\w/$]+)\\.(<init>|[\\w$]+)\\((Unknown Source|\\.dynamic|Native Method|[\\w.$]+:\\d+)\\)(?: ~?\\[.*:.*])?(?: \\{.*})?");

	public static final String[] JAVA_AND_JS_KEYWORDS = new String[]{
			// Common //
			"throw",
			"try",
			"catch",
			"return",
			"break",
			"continue",
			"if",
			"while",
			"for",
			"do",
			"else",
			"switch",
			"case",
			"true",
			"false",
			"new",
			"var",
			"this",
			// Java //
			"public",
			"private",
			"protected",
			"abstract",
			"static",
			"final",
			"transient",
			"volatile",
			"throws",
			"class",
			"interface",
			"@interface",
			"record",
			"enum",
			"extends",
			"implements",
			"super",
			"import",
			"package",
			"void",
			"byte",
			"short",
			"int",
			"long",
			"float",
			"double",
			"char",
			"String",
			// JS //
			"let",
			"const",
			"console",
			"function"
	};

	// 1 - strings
	// 2 - numbers
	// 3 - keywords
	// 4 - symbols
	// 5 - bracket open
	// 6 - bracket close
	public static final Pattern JAVA_AND_JS_PATTERN = Pattern.compile("(([\"'`]).*?\\2)|(\\d+(?:\\.\\d+)?)|(\\b(?:" + String.join("|", JAVA_AND_JS_KEYWORDS) + ")\\b)|([;=\\-+*/%&|^~!:?.,])|([{(<\\[])|([})>\\]])");

	public static final int TYPE_NONE = 0;
	public static final int TYPE_JAVA_AND_JS = 1;

	public static Response file(ServerRequest request) throws Exception {
		Snowflake channel = request.getSnowflake("channel");
		Snowflake id = request.getSnowflake("id");
		String filename = request.variable("filename");
		Paste.createPaste(request.app.db, channel.asLong(), id.asLong(), filename, "");
		return Redirect.permanently(App.url("paste/" + id.asString()));
	}

	public static Response pasteRaw(ServerRequest request) throws Exception {
		Snowflake id = request.getSnowflake("id");

		Paste paste = request.app.db.pastes.query(id.asLong()).first();

		if (paste == null) {
			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}

		Snowflake channel = Snowflake.of(paste.getChannelID());
		String filename = paste.getFilename();

		String contents;

		try {
			contents = URLRequest.of(Paste.getOriginalUrl(channel.asString(), id.asString(), filename)).toJoinedString().block().trim();
		} catch (Exception ex) {
			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}

		if (contents.isEmpty()) {
			throw HTTPResponseCode.NOT_FOUND.error("File is empty!");
		}

		BufferedReader reader = new BufferedReader(new StringReader(contents));

		StringBuilder sb = new StringBuilder();

		List<String> lines = new ArrayList<>();
		String line;

		while ((line = reader.readLine()) != null) {
			lines.add(line);
		}

		for (String s : lines) {
			sb.append(s);
			sb.append('\n');
		}

		return FileResponse.plainText(sb.toString())
				.withHeader("Gnome-Paste-Filename", filename)
				.withHeader("Gnome-Paste-Channel", channel.asString())
				.withHeader("Gnome-Paste-User", paste.getUser())
				;
	}

	public static Response paste(ServerRequest request) throws Exception {
		Snowflake id = request.getSnowflake("id");
		String contents;
		String filename;
		String user;

		try {
			URLRequest<String> req = Utils.internalRequest("paste/" + id.asString() + "/raw").toJoinedString();
			contents = req.block().trim();
			filename = req.getHeader("Gnome-Paste-Filename");
			user = req.getHeader("Gnome-Paste-User");
		} catch (Exception ex) {
			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}

		if (contents.isEmpty()) {
			throw HTTPResponseCode.NOT_FOUND.error("File is empty!");
		}

		RootTag root = RootTag.create();
		root.setupHead(request.getPath(), filename);
		Tag body = root.paired("body");
		Tag content = body.div().addClass("content");

		content.h3().string(filename + " by " + user).a("/paste/" + id.asString() + "/raw").string(" [Raw]").end();
		content.br();

		Tag pasteText = content.div().addClass("pastetext");

		String[] lines = contents.split("\n");

		String lineFormat = "%0" + String.valueOf(lines.length).length() + "d";
		StringBuilder sb = new StringBuilder();

		int fileType;

		if (filename.endsWith(".java") || filename.endsWith(".js") || filename.endsWith(".json")) {
			fileType = TYPE_JAVA_AND_JS;
		} else {
			fileType = TYPE_NONE;
		}

		for (int i = 0; i < lines.length; i++) {
			if (i == 50000) {
				pasteText.br();
				pasteText.p().addClass("error").string("This paste is too large! Rest of it will not have formatting!").end();
				pasteText.br();
			}

			String lineId = "L" + (i + 1);
			Tag line = pasteText.p();
			line.attr("id", lineId);

			if (fileType != TYPE_NONE) {
				line.addClass("info");
			} else if (lines[i].contains("ERR")) {
				line.addClass("error");
			} else if (lines[i].contains("WARN")) {
				line.addClass("warn");
			} else if (lines[i].contains("DEBUG") || lines[i].contains("TRACE")) {
				line.addClass("debug");
			} else if (lines[i].contains("Error:") || lines[i].contains("Exception:") || lines[i].contains("Caused by:") || lines[i].contains("Stacktrace:")) {
				line.addClass("error");
			} else {
				line.addClass("info");
			}

			line.a("#" + lineId).string(String.format(lineFormat, i + 1));
			line.a("").string("    ");

			if (i >= 50000) {
				line.string(lines[i]);
				continue;
			}

			if (fileType == TYPE_JAVA_AND_JS) {
				Matcher matcher = JAVA_AND_JS_PATTERN.matcher(lines[i]);

				while (matcher.find()) {
					sb.setLength(0);
					matcher.appendReplacement(sb, "");
					line.string(sb.toString());

					String string = matcher.group(1);
					String number = matcher.group(3);
					String keyword = matcher.group(4);
					String symbol = matcher.group(5);
					String bracketOpen = matcher.group(6);
					String bracketClose = matcher.group(7);

					if (string != null) {
						line.span("green").string(string);
					} else if (number != null) {
						line.span("orange").string(number);
					} else if (keyword != null) {
						line.span("magenta").string(keyword);
					} else if (symbol != null) {
						line.span("blue").string(symbol);
					} else if (bracketOpen != null) {
						line.span("blue").string(bracketOpen);
					} else if (bracketClose != null) {
						line.span("blue").string(bracketClose);
					}
				}

				sb.setLength(0);
				matcher.appendTail(sb);
				line.string(sb.toString());
			} else {
				Matcher matcher = AT_PATTERN.matcher(lines[i]);

				while (matcher.find()) {
					sb.setLength(0);
					matcher.appendReplacement(sb, "");
					line.string(sb.toString());

					String at = matcher.group(1);
					String packagePath = matcher.group(2);
					String className = matcher.group(3);
					String methodName = matcher.group(4);
					String source = matcher.group(5);

					line.string(at);
					line.span("orange").string(packagePath);
					line.string(".");
					line.span("yellow").string(className);
					line.string(".");
					line.span("blue").string(methodName);
					line.string(":");

					Set<String> sourceSet = Arrays.stream(className.split("\\$")).collect(Collectors.toSet());

					String[] sourceS = source.split(":", 2);

					if (sourceS[0].equals("Native Method")) {
						line.span("purple").string("native");
					} else if (sourceS[0].equals("Unknown Source")) {
						line.span("purple").string("unknown");
					} else if (sourceS[0].equals(".dynamic")) {
						line.span("purple").string("dynamic");
					} else if (sourceS[0].equals("SourceFile")) {
						line.span("purple").string("SourceFile");
					} else if (sourceS.length == 2 && sourceSet.contains(sourceS[0].replace(".java", ""))) {
						line.span("purple").string("L" + sourceS[1]);
					} else {
						line.span("purple").string(source.replace(".java", ""));
					}
				}

				sb.setLength(0);
				matcher.appendTail(sb);
				line.string(sb.toString());
			}
		}

		return root.asResponse();
	}
}
