package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.Databases;
import dev.gnomebot.app.discord.MessageHandler;
import dev.gnomebot.app.server.AppRequest;
import dev.gnomebot.app.server.AppRootTag;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.MessageId;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.URLRequest;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.tinyhttp.content.MimeType;
import dev.latvian.apps.tinyhttp.http.response.HTTPResponse;
import dev.latvian.apps.tinyhttp.http.response.error.client.ForbiddenError;
import dev.latvian.apps.tinyhttp.http.response.error.client.NotFoundError;
import dev.latvian.apps.webutils.CodingUtils;
import dev.latvian.apps.webutils.data.Pair;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.AttachmentData;
import discord4j.discordjson.json.MessageData;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

	public static final String[] JAVA_AND_JS_KEYWORDS = new String[]{/* Common */"throw", "try", "catch", "return", "break", "continue", "if", "while", "for", "do", "else", "switch", "case", "true", "false", "new", "var", "this",/* Java */"public", "private", "protected", "abstract", "static", "final", "transient", "volatile", "throws", "class", "interface", "@interface", "record", "enum", "extends", "implements", "super", "import", "package", "void", "byte", "short", "int", "long", "float", "double", "char", "String", "Number",/* JS */"let", "const", "console", "function", "number",/* TS */"module", "export", "readonly", "constructor", "type", "any",};

	// 1 - strings
	// 2 - numbers
	// 3 - keywords
	// 4 - symbols
	// 5 - bracket open
	// 6 - bracket close
	public static final Pattern JAVA_AND_JS_PATTERN = Pattern.compile("(([\"'`]).*?\\2)|(\\d+(?:\\.\\d+)?)|(\\b(?:" + String.join("|", JAVA_AND_JS_KEYWORDS) + ")\\b)|([;=\\-+*/%&|^~!:?.,])|([{(<\\[])|([})>\\]])");
	public static final Pattern STACK_AT_PATTERN = Pattern.compile("([\\s\\t]+at )(?:([\\w\\-./$@\\s*+]+)/)?([\\w.$@]+)\\.([\\w/$]+)\\.(<init>|<clinit>|[\\w$]+)\\((Unknown Source|\\.dynamic|Native Method|[\\w.$]+:\\d+)\\)(?: ~?\\[.*:.*])?(?: \\{.*})?");
	public static final Pattern MCLOGS_PATTERN = Pattern.compile("https://mclo.gs/(\\w+)");

	public static final Pattern MC_UUID_PATTERN = Pattern.compile("--uuid,\\s?(\\w{32})");
	public static final Pattern MC_USERNAME_PATTERN = Pattern.compile("--username,\\s?(\\w+)");

	public static HTTPResponse paste(AppRequest req) {
		var channelId = req.getSnowflake("channel");
		var messageId = req.getSnowflake("message");
		var id0 = req.variable("id").asString().split("!", 2);
		var id = SnowFlake.num(id0[0]);
		var zipPath = id0.length == 2 ? CodingUtils.decodeURL(id0[1]) : "";

		MessageData message;

		try {
			message = Objects.requireNonNull(req.app.discordHandler.client.getRestClient().getChannelService().getMessage(channelId, messageId).block());
		} catch (Exception ex) {
			Log.error("Message " + messageId + " not found!");
			throw new NotFoundError("Message " + messageId + " not found! " + ex.getMessage());
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
			throw new NotFoundError("File not found!");
		}

		var root = req.createRoot(attachment.filename());
		root.head.deferScript("/assets/paste.js" + AppRootTag.RESOURCE_REFRESH); // + "_%08x".formatted(UUID.randomUUID().hashCode()));

		root.content.h3().id("paste-title").string(attachment.filename() + " by " + message.author().username()).a(attachment.url(), " [Download]");
		root.content.br();
		root.content.classes("paste");

		var contentType = attachment.contentType().toOptional().orElse("");

		root.content.div()
				.id("paste-text")
				.classes("pastetext")
				.attr("data-paste-filename", attachment.filename())
				.attr("data-paste-url", attachment.url())
				.attr("data-paste-content-type", contentType.isEmpty() ? MimeType.TEXT : contentType)
				.attr("data-zip-path", zipPath)
				.h3().spanstr("Loading...");


		return root.asResponse().publicCache(Duration.ofMinutes(5L));
	}

	public static HTTPResponse oldPaste(AppRequest req) throws Exception {
		if (req.userAgent().contains("PetalBot")) {
			throw new ForbiddenError("Access Denied");
		}

		var channelId = req.getSnowflake("channel");
		var messageId = req.getSnowflake("message");
		var id0 = req.variable("id").asString().split("!", 2);
		var id = SnowFlake.num(id0[0]);
		var subfile = id0.length == 2 ? CodingUtils.decodeURL(id0[1]) : "";

		MessageData message;

		try {
			message = Objects.requireNonNull(req.app.discordHandler.client.getRestClient().getChannelService().getMessage(channelId, messageId).block());
		} catch (Exception ex) {
			Log.error("Message " + messageId + " not found!");
			throw new NotFoundError("Message " + messageId + " not found! " + ex.getMessage());
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
			throw new NotFoundError("File not found!");
		}

		String filename = attachment.filename();
		String user = message.author().username();
		byte[] contents;

		try {
			contents = URLRequest.of(attachment.url()).toBytes().block();
		} catch (Exception ex) {
			throw new NotFoundError("File not found!");
		}

		if (contents == null || contents.length == 0) {
			throw new NotFoundError("File is empty!");
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

			throw new NotFoundError("File not found!");
		}

		var url = "/paste/" + channelId + "/" + messageId + "/" + id;
		var rawUrl = attachment.url();
		return paste0(req, filename, contents, user, url, rawUrl, archive, subfile).publicCache(Duration.ofMinutes(5L));
	}

	public static HTTPResponse pasteMclogs(AppRequest req) {
		var id = req.variable("id").asString();
		var filename = "mclo.gs/" + id;

		var root = req.createRoot(filename);
		root.head.deferScript("/assets/paste.js" + AppRootTag.RESOURCE_REFRESH); // + "_%08x".formatted(UUID.randomUUID().hashCode()));

		root.content.h3().id("paste-title").string(filename).a("https://api.mclo.gs/1/raw/" + id, " [Download]");
		root.content.br();
		root.content.classes("paste");

		root.content.div()
				.id("paste-text")
				.classes("pastetext")
				.attr("data-paste-filename", filename)
				.attr("data-paste-url", "https://api.mclo.gs/1/log/" + id + "?raw&insights")
				.attr("data-paste-content-type", MimeType.TEXT)
				.attr("data-zip-path", "")
				.h3().spanstr("Loading...");


		return root.asResponse().publicCache(Duration.ofMinutes(5L));
	}

	private static HTTPResponse paste0(AppRequest req, String filename, byte[] contents, String user, String url, String rawUrl, boolean archive, String subfile) throws Exception {
		if (filename.endsWith(".pdf")) {
			return HTTPResponse.ok().content(contents, MimeType.PDF);
		}

		var root = req.createRoot(filename);

		var titleTag = root.content.h3().string(filename + " by " + user).a(rawUrl, " [Download]");
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
			return HTTPResponse.ok().content(contents, "image/" + filename.substring(filename.lastIndexOf('.') + 1));
		} else if (filename.endsWith(".mp4") || filename.endsWith(".avi") || filename.endsWith(".webm")) {
			// return pasteText.img("data:image/" + filename.substring(filename.lastIndexOf('.') + 1) + ";base64," + Base64.getEncoder().encodeToString(contents));
			return HTTPResponse.ok().content(contents, "video/" + filename.substring(filename.lastIndexOf('.') + 1));
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

			boolean isLanguage = filename.endsWith(".java") || filename.endsWith(".js") || filename.endsWith(".ts") || filename.endsWith(".zs") || filename.endsWith(".json");

			var mcUUID = "";
			var mcUsername = "";

			for (var i = 0; i < lines.size(); i++) {
				var line = lines.get(i);

				if (mcUUID.isEmpty()) {
					var matcher = MC_UUID_PATTERN.matcher(line);

					if (matcher.find()) {
						mcUUID = matcher.group(1);
					}
				}

				if (mcUsername.isEmpty()) {
					var matcher = MC_USERNAME_PATTERN.matcher(line);

					if (matcher.find()) {
						mcUsername = matcher.group(1);
					}
				}

				var lineId = "L" + (i + 1);
				var lineTag = pasteText.span();
				lineTag.id(lineId);

				if (i < MAX_LINES && !isLanguage) {
					if (line.contains("ERR")) {
						lineTag.classes("c-e");
					} else if (line.contains("WARN")) {
						lineTag.classes("c-w");
					} else if (line.contains("DEBUG") || line.contains("TRACE")) {
						lineTag.classes("c-d");
					} else if (line.contains("Error:") || line.contains("Exception:") || line.contains("Caused by:") || line.contains("Stacktrace:")) {
						lineTag.classes("c-e");
					}
				}

				lineTag.a("#" + lineId).string(String.format(lineFormat, i + 1));
				lineTag.a("").string("    ");

				if (i >= MAX_LINES) {
					lineTag.string(line);
					continue;
				}

				Matcher matcher;

				if (isLanguage) {
					matcher = JAVA_AND_JS_PATTERN.matcher(line);

					while (matcher.find()) {
						sb.setLength(0);
						matcher.appendReplacement(sb, "");
						lineTag.string(sb.toString());

						var string = matcher.group(1);
						var number = matcher.group(3);
						var keyword = matcher.group(4);
						var symbol = matcher.group(5);
						var bracketOpen = matcher.group(6);
						var bracketClose = matcher.group(7);

						if (string != null) {
							lineTag.span("f-g").string(string);
						} else if (number != null) {
							lineTag.span("f-o").string(number);
						} else if (keyword != null) {
							lineTag.span("f-m").string(keyword);
						} else if (symbol != null) {
							lineTag.span("f-b").string(symbol);
						} else if (bracketOpen != null) {
							lineTag.span("f-b").string(bracketOpen);
						} else if (bracketClose != null) {
							lineTag.span("f-b").string(bracketClose);
						}
					}
				} else {
					matcher = STACK_AT_PATTERN.matcher(line);

					while (matcher.find()) {
						sb.setLength(0);
						matcher.appendReplacement(sb, "");
						lineTag.string(sb.toString());

						var at = matcher.group(1);
						var moduleName = matcher.group(2);
						var packagePath = matcher.group(3);
						var className = matcher.group(4);
						var methodName = matcher.group(5);
						var source = matcher.group(6);

						lineTag.string(at);

						var pspan = lineTag.span("f-o").string(packagePath);

						if (moduleName != null) {
							pspan.title(moduleName);
						}

						lineTag.string(".");
						lineTag.span("f-y").string(className);
						lineTag.string(".");
						lineTag.span("f-b").string(methodName);
						lineTag.string(":");

						var sourceSet = Arrays.stream(className.split("\\$")).collect(Collectors.toSet());

						var sourceS = source.split(":", 2);

						var lineSpan = lineTag.span("f-p");

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

						lineSpan.title(line);
					}
				}

				sb.setLength(0);
				matcher.appendTail(sb);
				lineTag.string(sb.toString());
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

	public static String getUrl(App app, long channelId, long messageId, long attachmentId) {
		return app.url("paste/" + channelId + "/" + messageId + "/" + attachmentId);
	}

	public static void pasteMessage(Databases db, MessageChannel outputMessageChannel, Message m, String content, List<Attachment> attachments) {
		var buttons = new ArrayList<Button>();

		for (var attachment : attachments) {
			buttons.add(Button.link(getUrl(db.app, m.getChannelId().asLong(), m.getId().asLong(), attachment.getId().asLong()), "View " + attachment.getFilename()));
		}

		var mcLogsMatcher = MCLOGS_PATTERN.matcher(content);

		while (mcLogsMatcher.find()) {
			var id = mcLogsMatcher.group(1);
			buttons.add(Button.link(db.app.url("paste/mclogs/" + id), "View mclo.gs/" + id));
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
