package dev.gnomebot.app.discord.legacycommand.bot;

import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.command.ApplicationCommandEventWrapper;
import dev.gnomebot.app.discord.legacycommand.CommandCallback;
import dev.gnomebot.app.discord.legacycommand.LegacyDiscordCommand;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.Utils;
import discord4j.core.spec.EmbedCreateSpec;

/**
 * @author LatvianModder
 */
public class ReplyEmbedCommand {
	@LegacyDiscordCommand(name = "reply_embed", arguments = "<text...>", permissionLevel = AuthLevel.BOT)
	public static final CommandCallback COMMAND = (context, reader) -> {
		String text = Utils.trimEmbedDescription(reader.readRemainingString().orElse("")
				.replaceAll("role:(\\d+)", "<@&$1>")
				.replaceAll("user:(\\d+)", "<@$1>")
				.replace("mention:here", "@here")
				.replace("mention:everyone", "@everyone"));

		context.referenceMessage = false;

		if (context.interaction instanceof ComponentEventWrapper) {
			((ComponentEventWrapper) context.interaction).respond(builder -> builder.addEmbed(EmbedCreateSpec.builder().description(text).build()));
		} else if (context.interaction instanceof ApplicationCommandEventWrapper) {
			((ApplicationCommandEventWrapper) context.interaction).acknowledgeEphemeral();
			((ApplicationCommandEventWrapper) context.interaction).respond(builder -> builder.addEmbed(EmbedCreateSpec.builder().description(text).build().asRequest()));
		} else {
			context.reply(spec -> spec.description(text));
		}
	};
}
