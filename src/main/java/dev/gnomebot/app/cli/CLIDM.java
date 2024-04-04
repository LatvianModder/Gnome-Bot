package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.DM;

public class CLIDM {
	public static final CLICommand COMMAND = CLICommand.make("dm")
			.description("Sends a DM to a user")
			.run(CLIDM::run);

	// add counter_leaderboard

	private static void run(CLIEvent event) throws Exception {
		var user = event.reader.readUser().orElse(null);

		if (user == null) {
			event.respond("User not found!");
			return;
		}

		var message = event.reader.readString().orElse("Test");

		if (DM.send(event.gc.db.app.discordHandler, user.getUserData(), String.format("%s send you a test DM from %s: %s", event.sender.getMention(), event.gc, message), true).isPresent()) {
			event.respond("DM send!");
		} else {
			event.respond("Couldn't send a DM to this user!");
		}
	}
}
