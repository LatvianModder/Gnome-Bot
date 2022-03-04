package dev.gnomebot.app.cli;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.command.RegisterCommand;

public class CLIAdvancedLoggingAll {
	@RegisterCommand
	public static final CLICommand COMMAND = CLICommand.make("advanced_logging_all")
			.description("Toggles advanced logging in all guilds")
			.ownerOnly()
			.run(CLIAdvancedLoggingAll::run);

	private static void run(CLIEvent event) {
		event.gc.advancedLogging = !event.gc.advancedLogging;
		event.respond("Advanced logging: " + event.gc.advancedLogging);

		for (GuildCollections gc : event.gc.db.guildCollections.values()) {
			gc.advancedLogging = event.gc.advancedLogging;
		}
	}
}
