package dev.gnomebot.app.discord.command;

import discord4j.core.object.component.TextInput;

/**
 * @author LatvianModder
 */
public class PingsCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("pings")
			.description("Manage pings")
			.run(PingsCommand::run);

	private static void run(ApplicationCommandEventWrapper event) {
		event.respondModal("pings", "Manage Pings", TextInput.paragraph("pings", "Pings").placeholder("Run `/about pings` for info on how to set up pings"));
	}
}