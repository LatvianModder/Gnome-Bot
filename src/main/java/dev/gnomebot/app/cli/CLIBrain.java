package dev.gnomebot.app.cli;

import dev.gnomebot.app.Logger;
import dev.gnomebot.app.discord.command.RegisterCommand;

public class CLIBrain {
	@RegisterCommand
	public static final CLICommand COMMAND = CLICommand.make("brain")
			.description("Gnome brain")
			.run(CLIBrain::run);

	private static void run(CLIEvent event) {
		StringBuilder sb = new StringBuilder("Gnome Brain:\n```ansi\n");

		for (String brain : Logger.BRAIN) {
			sb.append(brain);

			if (brain.isEmpty()) {
				break;
			}
		}

		if (sb.length() > 1997) {
			sb.setLength(1997);
		}

		sb.append("```");
		event.respond(sb.toString());
	}
}
