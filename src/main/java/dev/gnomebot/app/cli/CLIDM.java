package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.command.RootCommand;
import discord4j.core.object.entity.User;

public class CLIDM {
	@RootCommand
	public static final CLICommand COMMAND = CLICommand.make("dm")
			.description("Sends a DM to a user")
			.run(CLIDM::run);

	// add counter_leaderboard

	private static void run(CLIEvent event) throws Exception {
		User user = event.reader.readUser().orElse(null);

		if (user == null) {
			event.respond("User not found!");
			return;
		}

		String message = event.reader.readString().orElse("Test");

		if (DM.send(event.gc.db.app.discordHandler, user, String.format("%s send you a test DM from %s: %s", event.sender.getMention(), event.gc, message), true).isPresent()) {
			event.respond("DM send!");
		} else {
			event.respond("Couldn't send a DM to this user!");
		}
	}
}
