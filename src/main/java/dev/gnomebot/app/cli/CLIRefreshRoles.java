package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.RootCommand;

public class CLIRefreshRoles {
	@RootCommand
	public static final CLICommand COMMAND = CLICommand.make("refresh_roles")
			.description("Run this command after you change XP/message requirements for regular roles")
			.run(CLIRefreshRoles::run);

	private static void run(CLIEvent event) {
		event.respond("Refreshing roles...");
	}
}
