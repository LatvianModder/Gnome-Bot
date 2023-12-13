package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.discord.ModalEventWrapper;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.core.object.component.TextInput;

public class ModmailCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("modmail")
			.description("Open a form that will send a message to server owners in a private channel")
			.run(ModmailCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) {
		if (event.context.gc.adminMessagesChannel.isSet()) {
			event.respondModal("modmail", "Send a message to server owners", TextInput.paragraph("message", "Message", "Write your message here! Please, don't send joke messages."));
		} else {
			event.respond("Modmail channel not set! You'll have to DM someone.");
		}
	}

	public static void modmailCallback(ModalEventWrapper event) {
		event.respond("Message sent!");

		String message = event.get("message").asString();

		event.context.gc.adminMessagesChannel.messageChannel().flatMap(ChannelInfo::getWebHook).ifPresent(w -> w.execute(MessageBuilder.create()
				.webhookName("Modmail from " + event.context.sender.getTag())
				.webhookAvatarUrl(event.context.sender.getAvatarUrl())
				.allowUserMentions(event.context.sender.getId())
				.content(event.context.sender.getMention() + ":\n" + message)
		));
	}
}
