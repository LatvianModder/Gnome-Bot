package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.Paste;
import dev.gnomebot.app.server.GnomeRootTag;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.FormattingUtils;
import dev.latvian.apps.webutils.html.Tag;
import dev.latvian.apps.webutils.net.FileResponse;
import dev.latvian.apps.webutils.net.Response;
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
	public static final int MAX_LINES = 50_000;

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
			"Number",
			// JS //
			"let",
			"const",
			"console",
			"function",
			"number",
			// TS //
			"module",
			"export",
			"readonly",
			"constructor",
			"type",
			"any",
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
		return Response.permanentRedirect(App.url("paste/" + id.asString()));
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

	public static Response pasteDirect(ServerRequest request) throws Exception {
		var channel = request.getSnowflake("channel");
		var id = request.getSnowflake("id");
		var name = request.variable("name");
		var author = request.variable("author");
		Paste.createPaste(request.app.db, channel.asLong(), id.asLong(), name, author);
		return Response.redirect("/paste/" + id.asString());
	}

	public static Response paste(ServerRequest request) throws Exception {
		Snowflake id = request.getSnowflake("id");
		String contents;
		String filename;
		String user;

		try {
			URLRequest<String> req = Utils.internalRequest("paste/" + id.asString() + "/raw").toJoinedString();
			contents = req.block();
			filename = req.getHeader("Gnome-Paste-Filename");
			user = req.getHeader("Gnome-Paste-User");
		} catch (Exception ex) {
			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}

		if (contents.isEmpty()) {
			throw HTTPResponseCode.NOT_FOUND.error("File is empty!");
		}

		var root = GnomeRootTag.createSimple("/paste/" + id.asString(), filename);

		root.content.h3().string(filename + " by " + user).a("/paste/" + id.asString() + "/raw").string(" [Raw]").end();
		root.content.br();

		Tag pasteText = root.content.div().classes("pastetext");

		String[] lines = contents.split("\n");

		String lineFormat = "%0" + String.valueOf(lines.length).length() + "d";
		StringBuilder sb = new StringBuilder();

		int fileType;

		if (filename.endsWith(".java") || filename.endsWith(".js") || filename.endsWith(".ts") || filename.endsWith(".zs") || filename.endsWith(".json")) {
			fileType = TYPE_JAVA_AND_JS;
		} else {
			fileType = TYPE_NONE;
		}

		if (lines.length > MAX_LINES) {
			pasteText.p().classes("error").string("This paste is too large! Only part of it will have formatting!").end();
			pasteText.br();
		}

		for (int i = 0; i < lines.length; i++) {
			String lineId = "L" + (i + 1);
			Tag line = pasteText.p();
			line.id(lineId);

			if (i >= MAX_LINES || fileType != TYPE_NONE) {
				line.classes("info");
			} else if (lines[i].contains("ERR")) {
				line.classes("error");
			} else if (lines[i].contains("WARN")) {
				line.classes("warn");
			} else if (lines[i].contains("DEBUG") || lines[i].contains("TRACE")) {
				line.classes("debug");
			} else if (lines[i].contains("Error:") || lines[i].contains("Exception:") || lines[i].contains("Caused by:") || lines[i].contains("Stacktrace:")) {
				line.classes("error");
			} else {
				line.classes("info");
			}

			line.a("#" + lineId).string(String.format(lineFormat, i + 1));
			line.a("").string("    ");

			if (i >= MAX_LINES) {
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
				Matcher matcher = FormattingUtils.STACK_AT_PATTERN.matcher(lines[i]);

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
