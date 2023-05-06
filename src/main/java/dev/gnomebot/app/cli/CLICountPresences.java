package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.RegisterCommand;
import dev.latvian.apps.webutils.FormattingUtils;

public class CLICountPresences {
	@RegisterCommand
	public static final CLICommand COMMAND = CLICommand.make("count_presences")
			.description("Counts presences")
			.run(CLICountPresences::run);

	// add counter_leaderboard

	private static void run(CLIEvent event) {
		int max = event.gc.getGuild().getMaxPresences();

		int count = event.gc.getGuild()
				.getPresences()
				.count()
				.block()
				.intValue();

		event.respond(FormattingUtils.format(count) + " / " + (max == 0 ? "?" : FormattingUtils.format(max)) + " presences");
	}
}
