package dev.gnomebot.app.cli;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.Macro;
import dev.gnomebot.app.discord.command.RootCommand;

public class CLICleanupDB {
	@RootCommand
	public static final CLICommand COMMAND = CLICommand.make("cleanup_db")
			.trustedOnly()
			.description("Cleans up old data")
			.run(CLICleanupDB::run);

	private static void run(CLIEvent event) {
		for (var id : event.gc.db.app.discordHandler.getSelfGuildIds()) {
			GuildCollections gc = event.gc.db.guild(id);

			for (Macro macro : gc.getMacroMap().values()) {
				if (!macro.document.getString("command_name").isEmpty()) {
					macro.update(Updates.unset("command_name"));
				}
			}
		}

		event.respond("Done");
	}
}
