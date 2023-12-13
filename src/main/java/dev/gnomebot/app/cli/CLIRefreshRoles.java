package dev.gnomebot.app.cli;

public class CLIRefreshRoles {
	public static final CLICommand COMMAND = CLICommand.make("refresh_roles")
			.description("Run this command after you change XP/message requirements for regular roles")
			.run(CLIRefreshRoles::run);

	private static void run(CLIEvent event) {
		event.respond("Refreshing roles...");
	}
}
