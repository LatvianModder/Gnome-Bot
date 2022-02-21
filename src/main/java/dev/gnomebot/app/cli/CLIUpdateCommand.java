package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.RootCommand;

public class CLIUpdateCommand {
	// TODO: Automate this with local cache of command signature

	@RootCommand
	public static final CLICommand COMMAND = CLICommand.make("update_command")
			.description("Update command")
			.ownerOnly()
			.run(CLIUpdateCommand::run);

	private static void run(CLIEvent event) throws Exception {
		if (event.gc.db.app.discordHandler.updateGlobalCommand(event.reader.readRemainingString().orElse(""))) {
			event.respond("Done!");
		} else {
			event.respond("Command not found!");
		}
	}
}
