package dev.gnomebot.app.discord.legacycommand.bot;

import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.command.ApplicationCommandEventWrapper;
import dev.gnomebot.app.discord.legacycommand.CommandCallback;
import dev.gnomebot.app.discord.legacycommand.LegacyDiscordCommand;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.Utils;

/**
 * @author LatvianModder
 */
public class ReactCommand {
	@LegacyDiscordCommand(name = "react", arguments = "<reaction>", permissionLevel = AuthLevel.BOT)
	public static final CommandCallback COMMAND = (context, reader) -> {
		String e = reader.readString().orElse("");

		if (context.interaction instanceof ComponentEventWrapper) {
			((ComponentEventWrapper) context.interaction).respond(e);
		} else if (context.interaction instanceof ApplicationCommandEventWrapper) {
			((ApplicationCommandEventWrapper) context.interaction).acknowledgeEphemeral();
			((ApplicationCommandEventWrapper) context.interaction).respond(e);
		} else {
			context.message.addReaction(Utils.stringToReaction(e)).block();
		}
	};

	@LegacyDiscordCommand(name = "upvote", permissionLevel = AuthLevel.BOT)
	public static final CommandCallback UPVOTE = (context, reader) -> {
		if (context.interaction instanceof ComponentEventWrapper) {
			((ComponentEventWrapper) context.interaction).respond("Success!");
		} else if (context.interaction instanceof ApplicationCommandEventWrapper) {
			((ApplicationCommandEventWrapper) context.interaction).acknowledgeEphemeral();
			((ApplicationCommandEventWrapper) context.interaction).respond("Success!");
		} else {
			context.message.addReaction(Emojis.VOTEUP).block();
		}
	};

	@LegacyDiscordCommand(name = "downvote", permissionLevel = AuthLevel.BOT)
	public static final CommandCallback DOWNVOTE = (context, reader) -> {
		if (context.interaction instanceof ComponentEventWrapper) {
			((ComponentEventWrapper) context.interaction).respond("Fail!");
		} else if (context.interaction instanceof ApplicationCommandEventWrapper) {
			((ApplicationCommandEventWrapper) context.interaction).acknowledgeEphemeral();
			((ApplicationCommandEventWrapper) context.interaction).respond("Fail!");
		} else {
			context.message.addReaction(Emojis.VOTEDOWN).block();
		}
	};
}
