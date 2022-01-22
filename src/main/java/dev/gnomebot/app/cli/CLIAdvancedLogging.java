package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.RootCommand;

public class CLIAdvancedLogging {
	@RootCommand
	public static final CLICommand COMMAND = CLICommand.make("advanced_logging")
			.description("Toggles advanced logging")
			.run(CLIAdvancedLogging::run);

	private static void run(CLIEvent event) {
		event.gc.advancedLogging = !event.gc.advancedLogging;
		event.respond("Advanced logging: " + event.gc.advancedLogging);
	}
}
