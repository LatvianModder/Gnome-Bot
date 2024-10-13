package dev.gnomebot.app.server.handler;

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
import dev.latvian.apps.webutils.ansi.Log;
import dev.latvian.apps.webutils.data.Pair;
import dev.latvian.apps.webutils.net.FileResponse;
import dev.latvian.apps.webutils.net.MimeType;
import dev.latvian.apps.webutils.net.Response;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.AttachmentData;
import discord4j.discordjson.json.MessageData;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HttpStatus;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
	public static final Pattern STACK_AT_PATTERN = Pattern.compile("([ \\t]+at )(?:([\\w./$@-]+)/)?([\\w.$@]+)\\.([\\w/$]+)\\.(<init>|[\\w$]+)\\((Unknown Source|\\.dynamic|Native Method|[\\w.$]+:\\d+)\\)(?: ~?\\[.*:.*])?(?: \\{.*})?");
	public static final Pattern MCLOGS_PATTERN = Pattern.compile("https://mclo.gs/(\\w+)");


	public static final Pattern WIN_USERNAME_PATTERN = Pattern.compile("\\b(\\w):([\\\\/])Users\\2(\\w+)\\2", Pattern.CASE_INSENSITIVE);
	public static final Pattern UNIX_USERNAME_PATTERN = Pattern.compile("/home/\\w+/");
	public static final Pattern MC_UUID_PATTERN = Pattern.compile("--uuid,\\s?(\\w{32})");
	public static final Pattern MC_USERNAME_PATTERN = Pattern.compile("--username,\\s?(\\w+)");

	public static final int TYPE_NONE = 0;
	public static final int TYPE_LANGUAGE = 1;

	public static Response pasteRaw(ServerRequest request) {
		var channelId = request.getSnowflake("channel");
		var messageId = request.getSnowflake("message");
		var id0 = request.variable("id").split("!", 2);
		var id = SnowFlake.num(id0[0]);

		MessageData message;

		try {
			message = Objects.requireNonNull(request.app.discordHandler.client.getRestClient().getChannelService().getMessage(channelId, messageId).block());
		} catch (Exception ex) {
			Log.error("Message " + messageId + " not found!");
			throw HTTPResponseCode.NOT_FOUND.error("Message " + messageId + " not found! " + ex.getMessage());
		}

		AttachmentData attachment = null;

		for (var att : message.attachments()) {
			if (att.id().asLong() == id) {
				attachment = att;
				break;
			}
		}

		if (attachment == null) {
			Log.error("Message " + messageId + " attachments: " + message.attachments().stream().map(a -> a.id().asString() + "/" + a.filename()).collect(Collectors.joining(", ")));
			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}

		var filename = attachment.filename();

		byte[] contents;

		try {
			contents = URLRequest.of(attachment.url()).toBytes().block();
		} catch (Exception ex) {
			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}

		if (contents == null || contents.length == 0) {
			throw HTTPResponseCode.NOT_FOUND.error("File is empty!");
		}

		var type = MimeType.TEXT;
		var download = false;

		if (filename.endsWith(".pdf")) {
			type = MimeType.PDF;
		} else if (filename.endsWith(".zip")) {
			type = MimeType.ZIP;
			download = true;
		} else if (filename.endsWith(".jar")) {
			type = MimeType.JAR;
			download = true;
		} else {
			var lines = new String(contents, StandardCharsets.UTF_8).split("\r?\n");

			for (int i = 0; i < lines.length; i++) {
				var s = lines[i];
				s = WIN_USERNAME_PATTERN.matcher(s).replaceAll("$1:$2Users$2<User>$2");
				s = UNIX_USERNAME_PATTERN.matcher(s).replaceAll("/home/<User>/");
				lines[i] = s;
			}

			contents = String.join("\n", lines).getBytes(StandardCharsets.UTF_8);
		}

		var response = FileResponse.of(HttpStatus.OK, type, contents)
				.withHeader("Gnome-Paste-Bytes", String.valueOf(contents.length))
				.withHeader("Gnome-Paste-Filename", filename)
				.withHeader("Gnome-Paste-Guild", message.guildId().toOptional().map(Id::asString).orElse(""))
				.withHeader("Gnome-Paste-Channel", SnowFlake.str(channelId))
				.withHeader("Gnome-Paste-Message", SnowFlake.str(messageId))
				.withHeader("Gnome-Paste-UserID", message.author().id().asString())
				.withHeader("Gnome-Paste-UserName", message.author().globalName().orElse(message.author().username()));

		if (download) {
			response = response.withHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
		}

		return response;
	}

	public static Response paste(ServerRequest request) throws Exception {
		if (request.userAgent.contains("PetalBot")) {
			throw new ForbiddenResponse("Access Denied");
		}

		var channel = request.getSnowflake("channel");
		var messageId = request.getSnowflake("message");
		var id0 = request.variable("id").split("!", 2);
		var id = id0[0];
		var subfile = id0.length == 2 ? CodingUtils.decodeURL(id0[1]) : "";
		byte[] contents;
		String filename;
		String user;

		try {
			var req = Utils.internalRequest("paste/" + channel + "/" + messageId + "/" + id + "/raw").toBytes();
			contents = req.block();
			filename = req.getHeader("Gnome-Paste-Filename");
			user = req.getHeader("Gnome-Paste-UserName");
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

		var url = "/paste/" + channel + "/" + messageId + "/" + id;
		var rawUrl = url + "/raw";
		return paste0(filename, contents, user, url, rawUrl, archive, subfile);
	}

	public static Response pasteMclogs(ServerRequest request) {
		var id = request.variable("id");

		try {
			var reqInfo = URLRequest.of("https://api.mclo.gs/1/insights/" + id).toJsonObject();

			try {
				var req = URLRequest.of("https://api.mclo.gs/1/raw/" + id).toBytes();
				return paste0(reqInfo.block().asString("title") + ".log", req.block(), "Unknown", "/paste/mclogs/" + id, "https://api.mclo.gs/1/raw/" + id, false, "");
			} catch (Exception ex) {
				throw HTTPResponseCode.NOT_FOUND.error("File not found!");
			}
		} catch (Exception ex) {
			throw HTTPResponseCode.NOT_FOUND.error("File not found!");
		}
	}

	private static Response paste0(String filename, byte[] contents, String user, String url, String rawUrl, boolean archive, String subfile) throws Exception {
		if (filename.endsWith(".pdf")) {
			return FileResponse.of(HttpStatus.OK, "application/pdf", contents);
		}

		var root = GnomeRootTag.createSimple(url, filename);
		root.head.deferScript("/assets/paste.js");

		var titleTag = root.content.h3().string(filename + " by " + user).a(rawUrl).string(archive ? " [Download]" : " [Raw]");
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
					zipList.add(Pair.of(zipEntry.getName(), url + "!" + CodingUtils.encodeURL(zipEntry.getName())));
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
				fileType = TYPE_LANGUAGE;
			} else {
				fileType = TYPE_NONE;
			}

			var mcUUID = "";
			var mcUsername = "";

			for (var i = 0; i < lines.size(); i++) {
				var s = lines.get(i);

				if (mcUUID.isEmpty()) {
					var matcher = MC_UUID_PATTERN.matcher(s);

					if (matcher.find()) {
						mcUUID = matcher.group(1);
					}
				}

				if (mcUsername.isEmpty()) {
					var matcher = MC_USERNAME_PATTERN.matcher(s);

					if (matcher.find()) {
						mcUsername = matcher.group(1);
					}
				}

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

				Matcher matcher;

				if (fileType == TYPE_LANGUAGE) {
					matcher = JAVA_AND_JS_PATTERN.matcher(s);

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
				} else {
					matcher = STACK_AT_PATTERN.matcher(s);

					while (matcher.find()) {
						sb.setLength(0);
						matcher.appendReplacement(sb, "");
						line.string(sb.toString());

						var at = matcher.group(1);
						var moduleName = matcher.group(2);
						var packagePath = matcher.group(3);
						var className = matcher.group(4);
						var methodName = matcher.group(5);
						var source = matcher.group(6);

						line.string(at);

						var pspan = line.span("f-o").string(packagePath);

						if (moduleName != null) {
							pspan.title(moduleName);
						}

						line.string(".");
						line.span("f-y").string(className);
						line.string(".");
						line.span("f-b").string(methodName);
						line.string(":");

						var sourceSet = Arrays.stream(className.split("\\$")).collect(Collectors.toSet());

						var sourceS = source.split(":", 2);

						var lineSpan = line.span("f-p");

						if (sourceS[0].equals("Native Method")) {
							lineSpan.string("native");
						} else if (sourceS[0].equals("Unknown Source")) {
							lineSpan.string("unknown");
						} else if (sourceS[0].equals(".dynamic")) {
							lineSpan.string("dynamic");
						} else if (sourceS[0].equals("SourceFile")) {
							lineSpan.string("SourceFile");
						} else if (sourceS.length == 2 && sourceSet.contains(sourceS[0].replace(".java", ""))) {
							lineSpan.string("L" + sourceS[1]);
						} else {
							lineSpan.string(source.replace(".java", ""));
						}

						lineSpan.title(s);
					}
				}

				sb.setLength(0);
				matcher.appendTail(sb);
				line.string(sb.toString());
			}

			if (!mcUUID.isEmpty() || !mcUsername.isEmpty()) {
				var mc = mcUUID.isEmpty() ? mcUsername : mcUUID;
				var a = titleTag.a("https://mcuuid.net/?q=" + mc).title(mcUsername);
				a.space();
				a.img("https://crafthead.net/avatar/" + mc + "/48").classes("inline-img");
				a.space();
				a.string("[Minecraft Profile]");
			}
		}

		return root.asResponse();
	}

	public static Response newPaste(ServerRequest request) throws Exception {
		var channel = request.getSnowflake("channel");
		var messageId = request.getSnowflake("message");
		var id0 = request.variable("id").split("!", 2);
		var id = id0[0];

		var root = GnomeRootTag.createSimple("/new-paste/" + channel + "/" + messageId + "/" + id, id + " - Paste");
		root.head.deferScript("/assets/paste.js");

		var rawTag = root.content.h3().span("", "Loading...").id("paste-title").end().space().a("/paste/" + channel + "/" + messageId + "/" + id + "/raw").id("paste-data").string(" [Raw]");

		if (id0.length == 2) {
			rawTag.attr("data-zippath", id0[1]);
		}

		root.content.br();
		root.content.classes("paste");
		root.content.div().id("paste-text").classes("pastetext");
		return root.asResponse();
	}

	public static String getUrl(long channelId, long messageId, long attachmentId) {
		return App.url("paste/" + channelId + "/" + messageId + "/" + attachmentId);
	}

	public static void pasteMessage(Databases db, MessageChannel outputMessageChannel, Message m, String content, List<Attachment> attachments) {
		var buttons = new ArrayList<Button>();

		for (var attachment : attachments) {
			buttons.add(Button.link(getUrl(m.getChannelId().asLong(), m.getId().asLong(), attachment.getId().asLong()), "View " + attachment.getFilename()));
		}

		var mcLogsMatcher = MCLOGS_PATTERN.matcher(content);

		while (mcLogsMatcher.find()) {
			var id = mcLogsMatcher.group(1);
			buttons.add(Button.link(App.url("paste/mclogs/" + id), "View mclo.gs/" + id));
		}

		if (buttons.isEmpty()) {
			return;
		}

		var userData = m.getUserData();

		if (userData.id().asLong() == db.app.discordHandler.selfId && (m.getData().components().isAbsent() || m.getData().components().get().isEmpty())) {
			m.edit(MessageBuilder.create()
					.addComponent(ActionRow.of(buttons))
					.toMessageEditSpec()
			).block();
		} else {
			var r = outputMessageChannel.createMessage(MessageBuilder.create()
					.content("Paste version of " + buttons.stream().map(a -> "`" + a.getLabel().orElse("?").replace("View ", "") + "`").collect(Collectors.joining(", ")) + " from <@" + userData.id().asString() + ">")
					.addComponent(ActionRow.of(buttons))
					.toMessageCreateSpec()
			).block();

			MessageHandler.addAutoDelete(m.getId().asLong(), new MessageId(r.getChannelId().asLong(), r.getId().asLong()));
		}
	}
}
