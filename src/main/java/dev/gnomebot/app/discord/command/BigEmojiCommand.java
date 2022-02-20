package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.util.Utils;
import discord4j.core.object.reaction.ReactionEmoji;

/**
 * @author LatvianModder
 */
public class BigEmojiCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("bigemoji")
			.description("Get image version of an emoji")
			.add(string("emoji").required())
			.run(BigEmojiCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledge();
		ReactionEmoji emoji = Utils.stringToReaction(event.get("emoji").asString());

		if (emoji instanceof ReactionEmoji.Custom custom) {
			if (custom.isAnimated()) {
				event.respond("https://cdn.discordapp.com/emojis/" + custom.getId().asString() + ".gif?quality=lossless");
			} else {
				event.respond("https://cdn.discordapp.com/emojis/" + custom.getId().asString() + ".png?v=1");
			}
		} else if (emoji instanceof ReactionEmoji.Unicode) {
			event.respond("Unicode emojis aren't supported!");
		} else {
			event.respond("Invalid emoji!");
		}
	}
}
