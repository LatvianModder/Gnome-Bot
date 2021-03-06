package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.Emojis;

/**
 * @author LatvianModder
 */
public class DecideCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("decide")
			.description("Decides fate")
			.add(string("text"))
			.run(DecideCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) {
		event.acknowledge();
		String s = event.get("text").asString().toLowerCase();
		long l = (s.isEmpty() ? System.currentTimeMillis() : s.replaceAll("\\W", "").hashCode()) & 1L;
		event.respond(l == 1L ? Emojis.GNOME_HAHA_YES.asFormat() : Emojis.GNOME_HAHA_NO.asFormat());
	}
}
