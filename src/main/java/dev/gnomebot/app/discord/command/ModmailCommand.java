package dev.gnomebot.app.discord.command;

import discord4j.core.object.component.TextInput;

/**
 * @author LatvianModder
 */
public class ModmailCommand extends ApplicationCommands {
	@RegisterCommand
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
}
