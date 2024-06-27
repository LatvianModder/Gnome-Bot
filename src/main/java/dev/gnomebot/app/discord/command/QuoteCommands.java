package dev.gnomebot.app.discord.command;

public class QuoteCommands extends ApplicationCommands {
	public static final MessageInteractionBuilder MESSAGE_INTERACTION = messageInteraction("Save Quote")
			.run(QuoteCommands::save);

	private static void save(MessageInteractionEventWrapper event) {
		event.respond("helo");
	}
}