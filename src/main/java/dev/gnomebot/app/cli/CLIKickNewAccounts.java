package dev.gnomebot.app.cli;

import dev.gnomebot.app.App;
import discord4j.core.object.entity.Member;

import java.time.Instant;

public class CLIKickNewAccounts {
	public static final CLICommand COMMAND = CLICommand.make("kick_new_accounts")
			.description("Kicks new accounts")
			.run(CLIKickNewAccounts::run);

	private static void run(CLIEvent event) {
		long kickSeconds = Math.min(event.reader.readSeconds().orElse(86400L), 604800L);
		long nowSecond = Instant.now().getEpochSecond();
		long kicked = 0L;

		for (Member member : event.gc.getMembers()) {
			long accountAge = nowSecond - member.getId().getTimestamp().getEpochSecond();

			if (accountAge <= kickSeconds) {
				App.info("Kicked " + member.getTag());
				member.kick("New Account").subscribe();
				kicked++;
			}
		}

		event.respond("Kicked " + kicked + " members");
	}
}
