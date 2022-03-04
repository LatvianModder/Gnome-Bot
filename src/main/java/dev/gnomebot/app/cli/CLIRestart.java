package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.RegisterCommand;

public class CLIRestart {
	@RegisterCommand
	public static final CLICommand COMMAND = CLICommand.make("restart")
			.trustedOnly()
			.description("Restarts the bot")
			.run(CLIRestart::run);

	private static void run(CLIEvent event) {
		event.respond("Restarting...");
		event.gc.db.app.restart();
	}
}
