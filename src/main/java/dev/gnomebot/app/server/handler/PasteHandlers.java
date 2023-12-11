package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.Paste;
import dev.gnomebot.app.server.GnomeRootTag;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.CodingUtils;
import dev.latvian.apps.webutils.FormattingUtils;
import dev.latvian.apps.webutils.data.Pair;
import dev.latvian.apps.webutils.html.Tag;
import dev.latvian.apps.webutils.net.FileResponse;
import dev.latvian.apps.webutils.net.MimeType;
import dev.latvian.apps.webutils.net.Response;
import discord4j.common.util.Snowflake;
import io.javalin.http.HttpStatus;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
		var id0 = request.variable("id").split("!", 2);
		var id = Snowflake.of(id0[0]);

		Paste paste = request.app.db.pastes.query(id.asLong()).first();

		if (paste == null) {
			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}

		Snowflake channel = Snowflake.of(paste.getChannelID());
		String filename = paste.getFilename();

		byte[] contents;

		try {
			contents = URLRequest.of(Paste.getOriginalUrl(channel.asString(), id.asString(), filename)).toBytes().block();
		} catch (Exception ex) {
			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}

		if (contents == null || contents.length == 0) {
			throw HTTPResponseCode.NOT_FOUND.error("File is empty!");
		}

		var type = MimeType.TEXT;
		boolean download = false;

		if (filename.endsWith(".pdf")) {
			type = "application/pdf";
		} else if (filename.endsWith(".zip")) {
			type = "application/zip";
			download = true;
		} else if (filename.endsWith(".jar")) {
			type = "application/java-archive";
			download = true;
		}

		var response = FileResponse.of(HttpStatus.OK, type, contents)
				.withHeader("Gnome-Paste-Bytes", String.valueOf(contents.length))
				.withHeader("Gnome-Paste-Filename", filename)
				.withHeader("Gnome-Paste-Channel", channel.asString())
				.withHeader("Gnome-Paste-User", paste.getUser());

		if (download) {
			response = response.withHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
		}

		return response;
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
		var id0 = request.variable("id").split("!", 2);
		var id = Snowflake.of(id0[0]);
		String subfile = id0.length == 2 ? CodingUtils.decodeURL(id0[1]) : "";
		byte[] contents;
		String filename;
		String user;

		try {
			URLRequest<byte[]> req = Utils.internalRequest("paste/" + id.asString() + "/raw").toBytes();
			contents = req.block();
			filename = req.getHeader("Gnome-Paste-Filename");
			user = req.getHeader("Gnome-Paste-User");
		} catch (Exception ex) {
			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}

		if (contents == null || contents.length == 0) {
			throw HTTPResponseCode.NOT_FOUND.error("File is empty!");
		}

		boolean archive = filename.endsWith(".zip") || filename.endsWith(".jar");

		zipExit:
		if (archive && !subfile.isEmpty()) {
			var stream = (filename.endsWith(".jar") ? new JarInputStream(new ByteArrayInputStream(contents)) : new ZipInputStream(new ByteArrayInputStream(contents)));
			ZipEntry zipEntry;

			while ((zipEntry = stream.getNextEntry()) != null) {
				if (zipEntry.getName().equals(subfile)) {
					filename = zipEntry.getName();

					if (filename.indexOf('/') != -1) {
						filename = filename.substring(filename.lastIndexOf('/') + 1);
					}

					contents = stream.readAllBytes();
					break zipExit;
				}
			}

			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}

		if (filename.endsWith(".pdf")) {
			return FileResponse.of(HttpStatus.OK, "application/pdf", contents);
		}

		var root = GnomeRootTag.createSimple("/paste/" + id.asString(), filename);

		root.content.h3().string(filename + " by " + user).a("/paste/" + id.asString() + "/raw").string(archive ? " [Download]" : " [Raw]").end();
		root.content.br();

		var pasteText = root.content.div().classes("pastetext");

		if (archive && subfile.isEmpty()) {
			var zipList = new ArrayList<Pair<String, String>>();

			var stream = (filename.endsWith(".jar") ? new JarInputStream(new ByteArrayInputStream(contents)) : new ZipInputStream(new ByteArrayInputStream(contents)));
			ZipEntry zipEntry;

			while ((zipEntry = stream.getNextEntry()) != null) {
				if (zipEntry.isDirectory() || zipEntry.getName().endsWith(".zip") || zipEntry.getName().endsWith(".jar")) {
					zipList.add(Pair.of(zipEntry.getName(), ""));
				} else {
					zipList.add(Pair.of(zipEntry.getName(), "/paste/" + id.asString() + "!" + CodingUtils.encodeURL(zipEntry.getName())));
				}
			}

			zipList.sort((o1, o2) -> o1.a().compareToIgnoreCase(o2.a()));

			for (var pair : zipList) {
				if (pair.b().isEmpty()) {
					pasteText.span().string(pair.a());
				} else {
					pasteText.span().a(pair.b(), pair.a());
				}
			}

			return root.asResponse();
		}

		if (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".gif") || filename.endsWith(".webp")) {
			// return pasteText.img("data:image/" + filename.substring(filename.lastIndexOf('.') + 1) + ";base64," + Base64.getEncoder().encodeToString(contents));
			return FileResponse.of(HttpStatus.OK, "image/" + filename.substring(filename.lastIndexOf('.') + 1), contents);
		} else if (filename.endsWith(".mp4") || filename.endsWith(".avi") || filename.endsWith(".webm")) {
			// return pasteText.img("data:image/" + filename.substring(filename.lastIndexOf('.') + 1) + ";base64," + Base64.getEncoder().encodeToString(contents));
			return FileResponse.of(HttpStatus.OK, "video/" + filename.substring(filename.lastIndexOf('.') + 1), contents);
		} else if (filename.endsWith(".html")) {
			pasteText.span().raw(new String(contents, StandardCharsets.UTF_8));
		} else {
			List<String> lines = new ArrayList<>();
			var reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(contents), StandardCharsets.UTF_8));
			String readerLine;

			while ((readerLine = reader.readLine()) != null) {
				if (lines.size() >= MAX_LINES) {
					lines.add("This paste is too large! Only part of it will have formatting!");
					break;
				} else {
					lines.add(readerLine);
				}
			}

			String lineFormat = "%0" + String.valueOf(lines.size()).length() + "d";
			StringBuilder sb = new StringBuilder();

			int fileType;

			if (filename.endsWith(".java") || filename.endsWith(".js") || filename.endsWith(".ts") || filename.endsWith(".zs") || filename.endsWith(".json")) {
				fileType = TYPE_JAVA_AND_JS;
			} else {
				fileType = TYPE_NONE;
			}

			for (int i = 0; i < lines.size(); i++) {
				var s = lines.get(i);
				String lineId = "L" + (i + 1);
				Tag line = pasteText.span();
				line.id(lineId);

				if (i < MAX_LINES && fileType == TYPE_NONE) {
					if (s.contains("ERR")) {
						line.classes("c-e");
					} else if (s.contains("WARN")) {
						line.classes("c-w");
					} else if (s.contains("DEBUG") || s.contains("TRACE")) {
						line.classes("c-d");
					} else if (s.contains("Error:") || s.contains("Exception:") || s.contains("Caused by:") || s.contains("Stacktrace:")) {
						line.classes("c-e");
					}
				}

				line.a("#" + lineId).string(String.format(lineFormat, i + 1));
				line.a("").string("    ");

				if (i >= MAX_LINES) {
					line.string(s);
					continue;
				}

				if (fileType == TYPE_JAVA_AND_JS) {
					Matcher matcher = JAVA_AND_JS_PATTERN.matcher(s);

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
							line.span("f-g").string(string);
						} else if (number != null) {
							line.span("f-o").string(number);
						} else if (keyword != null) {
							line.span("f-m").string(keyword);
						} else if (symbol != null) {
							line.span("f-b").string(symbol);
						} else if (bracketOpen != null) {
							line.span("f-b").string(bracketOpen);
						} else if (bracketClose != null) {
							line.span("f-b").string(bracketClose);
						}
					}

					sb.setLength(0);
					matcher.appendTail(sb);
					line.string(sb.toString());
				} else {
					Matcher matcher = FormattingUtils.STACK_AT_PATTERN.matcher(s);

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
						line.span("f-o").string(packagePath);
						line.string(".");
						line.span("f-y").string(className);
						line.string(".");
						line.span("f-b").string(methodName);
						line.string(":");

						Set<String> sourceSet = Arrays.stream(className.split("\\$")).collect(Collectors.toSet());

						String[] sourceS = source.split(":", 2);

						if (sourceS[0].equals("Native Method")) {
							line.span("f-p").string("native");
						} else if (sourceS[0].equals("Unknown Source")) {
							line.span("f-p").string("unknown");
						} else if (sourceS[0].equals(".dynamic")) {
							line.span("f-p").string("dynamic");
						} else if (sourceS[0].equals("SourceFile")) {
							line.span("f-p").string("SourceFile");
						} else if (sourceS.length == 2 && sourceSet.contains(sourceS[0].replace(".java", ""))) {
							line.span("f-p").string("L" + sourceS[1]);
						} else {
							line.span("f-p").string(source.replace(".java", ""));
						}
					}

					sb.setLength(0);
					matcher.appendTail(sb);
					line.string(sb.toString());
				}
			}
		}

		return root.asResponse();
	}
}
