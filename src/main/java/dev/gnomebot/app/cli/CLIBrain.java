package dev.gnomebot.app.cli;

import dev.gnomebot.app.Logger;

import java.util.ArrayList;
import java.util.Collections;

public class CLIBrain {
	public static final CLICommand COMMAND = CLICommand.make("brain")
			.description("Gnome brain")
			.run(CLIBrain::run);

	private static void run(CLIEvent event) {
		var sb = new StringBuilder("Gnome Brain:\n```ansi\n");
		var brain = new ArrayList<>(Logger.BRAIN);
		Collections.reverse(brain);

		for (var c : brain) {
			sb.append(c);
		}

		if (sb.length() > 1997) {
			sb.setLength(1997);
		}

		sb.append("```");
		event.respond(sb.toString());
	}
}
