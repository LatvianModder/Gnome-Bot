package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.RootCommand;

public class CLIAuthLevel {
	@RootCommand
	public static final CLICommand COMMAND = CLICommand.make("auth_level")
			.description("Print your auth level in Gnome Panel")
			.noAdmin()
			.run(CLIAuthLevel::run);

	private static void run(CLIEvent event) {
		event.respond("Your Gnome Panel auth level is `" + event.gc.getAuthLevel(event.sender) + "`");
	}
}
