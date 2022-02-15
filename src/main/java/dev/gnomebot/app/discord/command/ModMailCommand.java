package dev.gnomebot.app.discord.command;

import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.TextInput;

import java.util.Collections;

/**
 * @author LatvianModder
 */
public class ModMailCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("modmail")
			.description("Open a form that will send a message to server owners in a private channel")
			.run(ModMailCommand::run);

	private static void run(ApplicationCommandEventWrapper event) {
		event.presentModal("modmail", "Send a message to server owners", Collections.singletonList(ActionRow.of(TextInput.paragraph("message", "Message", "Write your message here! Please, don't send joke messages."))));
	}
}
