package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.Emojis;

public class DecideCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("decide")
			.description("Decides fate")
			.add(string("text"))
			.run(DecideCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) {
		event.acknowledge();
		var s = event.get("text").asString().replaceAll("\\W", "").toLowerCase();
		var l = (s.isEmpty() ? System.currentTimeMillis() : s.replaceAll("\\W", "").hashCode()) & 1L;

		var sb = new StringBuilder();

		if (!s.isEmpty()) {
			sb.append("### ");
			sb.append(event.get("text").asString());
			sb.append('\n');
		}

		sb.append("# ");

		if (l == 1L) {
			sb.append("Yes ");
			sb.append(Emojis.GNOME_HAHA_YES.asFormat());
		} else {
			sb.append("No ");
			sb.append(Emojis.GNOME_HAHA_NO.asFormat());
		}

		event.respond(sb.toString());
	}
}
