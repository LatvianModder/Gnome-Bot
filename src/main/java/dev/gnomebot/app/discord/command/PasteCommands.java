package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.Paste;
import dev.gnomebot.app.discord.ModalEventWrapper;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SnowFlake;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TextInput;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class PasteCommands extends ApplicationCommands {
	public static final Pattern URL_REGEX = Pattern.compile("https://(?:media|cdn)\\.(?:discordapp|discord)\\.(?:com|net)/attachments/(\\d+)/(\\d+)/(\\S+)");

	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("paste")
			.add(sub("create")
					.description("Create paste link for content from clipboard")
					.run(PasteCommands::create)
			)
			.add(sub("url")
					.description("Create paste link for discord attachment")
					.add(string("attachment-url").required())
					.run(PasteCommands::url)
			)
			.add(sub("message")
					.description("Find attachment from message link")
					.add(string("message-link").required())
					.run(PasteCommands::message)
			);

	private static void create(ChatInputInteractionEventWrapper event) {
		event.respondModal("create-paste", "Create Paste",
				TextInput.paragraph("content", "Content").required(true),
				TextInput.small("filename", "File name").required(true).prefilled("file.txt")
		);
	}

	public static void createCallback(ModalEventWrapper event) {
		event.respond(MessageBuilder.create().ephemeral(false).addFile(event.get("filename").asString(""), event.get("content").asString("").getBytes(StandardCharsets.UTF_8)));
	}

	private static void url(ChatInputInteractionEventWrapper event) {
		var mu = event.get("attachment-url").asString();

		var urlm = URL_REGEX.matcher(mu);

		if (urlm.find()) {
			event.acknowledge();

			var channelId = SnowFlake.num(urlm.group(1));
			var attachmentId = SnowFlake.num(urlm.group(2));
			var filename = urlm.group(3);

			Paste.createPaste(event.context.gc.db, channelId, attachmentId, filename, "");
			event.edit().respond(MessageBuilder.create("Paste version of `" + filename + "`").addComponentRow(Button.link(Paste.getUrl(attachmentId), "View " + filename)));
		}

		throw new GnomeException("Invalid attachment url!");
	}

	private static void message(ChatInputInteractionEventWrapper event) {
		throw wip();
	}
}
