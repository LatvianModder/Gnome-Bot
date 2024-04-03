package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.MessageHandler;
import dev.gnomebot.app.discord.ModalEventWrapper;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.server.handler.PasteHandlers;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SnowFlake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.TextInput;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PasteCommands extends ApplicationCommands {
	public static final Pattern URL_REGEX = Pattern.compile("https://(?:media|cdn)\\.(?:discordapp|discord)\\.(?:com|net)/attachments/(\\d+)/(\\d+)/([^\\s?]+)");

	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("paste")
			.supportsDM()
			.add(sub("create")
					.description("Create paste link for content from clipboard")
					.run(PasteCommands::create)
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
		event.respond(MessageBuilder.create().ephemeral(false).addFile(event.get("filename").asString("file.txt"), event.get("content").asString("").getBytes(StandardCharsets.UTF_8)));
	}

	private static void message(ChatInputInteractionEventWrapper event) {
		event.acknowledge();
		var matcher = MessageHandler.MESSAGE_URL_PATTERN.matcher(event.get("message-link").asString());

		if (matcher.find()) {
			var msg = App.instance.discordHandler.client.getMessageById(SnowFlake.convert(matcher.group(2)), SnowFlake.convert(matcher.group(3))).block();

			if (msg != null) {
				var attachments = msg.getAttachments();

				if (attachments.isEmpty()) {
					throw new GnomeException("No attachments found!");
				}

				var buttons = new ArrayList<Button>();

				long author = msg.getUserData().id().asLong();

				for (var att : attachments) {
					PasteHandlers.createPaste(App.instance.db, msg.getChannelId().asLong(), msg.getId().asLong(), att.getId().asLong(), att.getFilename(), author);
					buttons.add(Button.link(PasteHandlers.getUrl(att.getId().asLong()), "View " + att.getFilename()));
				}

				event.respond(MessageBuilder.create()
						.content("Paste version of " + attachments.stream().map(a -> "`" + a.getFilename() + "`").collect(Collectors.joining(", ")) + " from <@" + author + ">")
						.addComponent(ActionRow.of(buttons))
				);
			} else {
				throw new GnomeException("Message not found!");
			}
		}

		throw new GnomeException("Invalid message URL!");
	}
}
