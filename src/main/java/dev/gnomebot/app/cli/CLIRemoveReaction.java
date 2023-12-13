package dev.gnomebot.app.cli;

public class CLIRemoveReaction {
	public static final CLICommand COMMAND = CLICommand.make("remove_reaction")
			.description("Remove all of one reaction from a message")
			.run(CLIRemoveReaction::run);

	// add counter_leaderboard

	private static void run(CLIEvent event) {
		var cm = event.reader.readChannelAndMessage().get();
		var message = cm.a().getMessage(cm.b());
		var emoji = event.reader.readEmoji().get();
		message.removeReactions(emoji).block();
		event.respond("Done");
	}
}
