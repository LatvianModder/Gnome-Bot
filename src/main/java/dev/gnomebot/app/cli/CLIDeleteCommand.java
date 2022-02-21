package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.RootCommand;

public class CLIDeleteCommand {
	// TODO: Automate this with local cache of command signature

	@RootCommand
	public static final CLICommand COMMAND = CLICommand.make("delete_command")
			.description("Delete command")
			.ownerOnly()
			.run(CLIDeleteCommand::run);

	private static void run(CLIEvent event) {
		if (event.gc.db.app.discordHandler.deleteGlobalCommand(event.reader.readRemainingString().orElse(""))) {
			event.respond("Done!");
		} else {
			event.respond("Command not found!");
		}
	}
}
