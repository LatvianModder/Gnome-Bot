package dev.gnomebot.app.server.handler;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.data.Databases;
import dev.gnomebot.app.discord.MessageHandler;
import dev.gnomebot.app.server.GnomeRootTag;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.MessageId;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.URLRequest;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.CodingUtils;
import dev.latvian.apps.webutils.FormattingUtils;
import dev.latvian.apps.webutils.data.Pair;
import dev.latvian.apps.webutils.net.FileResponse;
import dev.latvian.apps.webutils.net.MimeType;
import dev.latvian.apps.webutils.net.Response;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import discord4j.rest.entity.RestChannel;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HttpStatus;
import org.bson.conversions.Bson;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarInputStream;
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

	public static Response pasteRaw(ServerRequest request) {
		var id0 = request.variable("id").split("!", 2);
		var id = SnowFlake.num(id0[0]);

		var paste = request.app.db.pastesDB.query(id).first();

		if (paste == null) {
			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}

		var channel = paste.getChannelID();
		var messageId = paste.getMessageID();

		var filename = paste.getFilename();

		Attachment attachment = null;

		for (var att : request.app.discordHandler.client.getMessageById(SnowFlake.convert(channel), SnowFlake.convert(messageId)).block().getAttachments()) {
			if (att.getId().asLong() == id) {
				attachment = att;
				break;
			}
		}

		if (attachment == null) {
			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}

		byte[] contents;

		try {
			contents = URLRequest.of(attachment.getUrl()).toBytes().block();
		} catch (Exception ex) {
			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}

		if (contents == null || contents.length == 0) {
			throw HTTPResponseCode.NOT_FOUND.error("File is empty!");
		}

		var type = MimeType.TEXT;
		var download = false;

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
				.withHeader("Gnome-Paste-Channel", SnowFlake.str(channel))
				.withHeader("Gnome-Paste-Message", SnowFlake.str(messageId))
				.withHeader("Gnome-Paste-User", paste.getUser());

		if (download) {
			response = response.withHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
		}

		return response;
	}

	public static Response paste(ServerRequest request) throws Exception {
		if (request.userAgent.contains("PetalBot")) {
			throw new ForbiddenResponse("Access Denied");
		}

		var id0 = request.variable("id").split("!", 2);
		var id = id0[0];
		var subfile = id0.length == 2 ? CodingUtils.decodeURL(id0[1]) : "";
		byte[] contents;
		String filename;
		String user;

		try {
			var req = Utils.internalRequest("paste/" + id + "/raw").toBytes();
			contents = req.block();
			filename = req.getHeader("Gnome-Paste-Filename");
			user = req.getHeader("Gnome-Paste-User");
		} catch (Exception ex) {
			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}

		if (contents == null || contents.length == 0) {
			throw HTTPResponseCode.NOT_FOUND.error("File is empty!");
		}

		var archive = filename.endsWith(".zip") || filename.endsWith(".jar");

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

		var root = GnomeRootTag.createSimple("/paste/" + id, filename);

		root.content.h3().string(filename + " by " + user).a("/paste/" + id + "/raw").string(archive ? " [Download]" : " [Raw]").end();
		root.content.br();
		root.content.classes("paste");

		var pasteText = root.content.div().classes("pastetext");

		if (archive && subfile.isEmpty()) {
			var zipList = new ArrayList<Pair<String, String>>();

			var stream = (filename.endsWith(".jar") ? new JarInputStream(new ByteArrayInputStream(contents)) : new ZipInputStream(new ByteArrayInputStream(contents)));
			ZipEntry zipEntry;

			while ((zipEntry = stream.getNextEntry()) != null) {
				if (zipEntry.isDirectory() || zipEntry.getName().endsWith(".zip") || zipEntry.getName().endsWith(".jar")) {
					zipList.add(Pair.of(zipEntry.getName(), ""));
				} else {
					zipList.add(Pair.of(zipEntry.getName(), "/paste/" + id + "!" + CodingUtils.encodeURL(zipEntry.getName())));
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

			var lineFormat = "%0" + String.valueOf(lines.size()).length() + "d";
			var sb = new StringBuilder();

			int fileType;

			if (filename.endsWith(".java") || filename.endsWith(".js") || filename.endsWith(".ts") || filename.endsWith(".zs") || filename.endsWith(".json")) {
				fileType = TYPE_JAVA_AND_JS;
			} else {
				fileType = TYPE_NONE;
			}

			for (var i = 0; i < lines.size(); i++) {
				var s = lines.get(i);
				var lineId = "L" + (i + 1);
				var line = pasteText.span();
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
					var matcher = JAVA_AND_JS_PATTERN.matcher(s);

					while (matcher.find()) {
						sb.setLength(0);
						matcher.appendReplacement(sb, "");
						line.string(sb.toString());

						var string = matcher.group(1);
						var number = matcher.group(3);
						var keyword = matcher.group(4);
						var symbol = matcher.group(5);
						var bracketOpen = matcher.group(6);
						var bracketClose = matcher.group(7);

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
					var matcher = FormattingUtils.STACK_AT_PATTERN.matcher(s);

					while (matcher.find()) {
						sb.setLength(0);
						matcher.appendReplacement(sb, "");
						line.string(sb.toString());

						var at = matcher.group(1);
						var packagePath = matcher.group(2);
						var className = matcher.group(3);
						var methodName = matcher.group(4);
						var source = matcher.group(5);

						line.string(at);
						line.span("f-o").string(packagePath);
						line.string(".");
						line.span("f-y").string(className);
						line.string(".");
						line.span("f-b").string(methodName);
						line.string(":");

						var sourceSet = Arrays.stream(className.split("\\$")).collect(Collectors.toSet());

						var sourceS = source.split(":", 2);

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

	public static String getUrl(long attachmentId) {
		return App.url("paste/" + attachmentId);
	}

	public static void pasteMessage(Databases db, RestChannel channel, Message m, List<Attachment> attachments) {
		if (attachments.isEmpty()) {
			return;
		}

		var userData = m.getUserData();
		var buttons = new ArrayList<Button>();

		for (var attachment : attachments) {
			createPaste(db, m.getChannelId().asLong(), m.getId().asLong(), attachment.getId().asLong(), attachment.getFilename(), userData.id().asLong());
			buttons.add(Button.link(getUrl(attachment.getId().asLong()), "View " + attachment.getFilename()));
		}

		if (userData.id().asLong() == db.app.discordHandler.selfId && (m.getData().components().isAbsent() || m.getData().components().get().isEmpty())) {
			m.edit(MessageBuilder.create()
					.addComponent(ActionRow.of(buttons))
					.toMessageEditSpec()
			).subscribe();
		} else {
			channel.createMessage(MessageBuilder.create()
					.content("Paste version of " + attachments.stream().map(a -> "`" + a.getFilename() + "`").collect(Collectors.joining(", ")) + " from <@" + userData.id().asString() + ">")
					.addComponent(ActionRow.of(buttons))
					.toMultipartMessageCreateRequest()
			).subscribe(m1 -> MessageHandler.addAutoDelete(m.getId().asLong(), new MessageId(m1.channelId().asLong(), m1.id().asLong())));
		}
	}

	public static void createPaste(Databases db, long channelId, long messageId, long attachmentId, String filename, long user) {
		var updates = new ArrayList<Bson>();
		updates.add(Updates.set("channel", channelId));
		updates.add(Updates.set("message", messageId));
		updates.add(Updates.set("filename", filename));

		if (user != 0L) {
			updates.add(Updates.set("user", user));
		}

		db.pastesDB.query(attachmentId).upsert(updates);
	}
}
