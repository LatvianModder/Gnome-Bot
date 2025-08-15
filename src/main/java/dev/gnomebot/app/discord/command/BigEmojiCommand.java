package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.util.Utils;
import discord4j.core.object.emoji.CustomEmoji;
import discord4j.core.object.emoji.UnicodeEmoji;

public class BigEmojiCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("bigemoji")
			.supportsDM()
			.description("Get image version of an emoji")
			.add(string("emoji").required())
			.run(BigEmojiCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledge();
		var emoji = Utils.stringToReaction(event.get("emoji").asString());

		if (emoji instanceof CustomEmoji custom) {
			if (custom.isAnimated()) {
				event.respond("https://cdn.discordapp.com/emojis/" + custom.getId().asString() + ".gif?quality=lossless");
			} else {
				event.respond("https://cdn.discordapp.com/emojis/" + custom.getId().asString() + ".png?v=1");
			}
		} else if (emoji instanceof UnicodeEmoji) {
			event.respond("Unicode emojis aren't supported!");
		} else {
			event.respond("Invalid emoji!");
		}
	}
}
