package dev.gnomebot.app.discord.legacycommand.bot;

import dev.gnomebot.app.discord.legacycommand.CommandCallback;
import dev.gnomebot.app.discord.legacycommand.LegacyDiscordCommand;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.core.spec.BanQuerySpec;

/**
 * @author LatvianModder
 */
public class BanCommand {
	@LegacyDiscordCommand(name = "ban", arguments = "<delete messages true/false> [reason...]", permissionLevel = AuthLevel.BOT)
	public static final CommandCallback COMMAND = (context, reader) -> {
		boolean deleteMessages = reader.readBoolean().orElse(false);
		String reason = reader.readRemainingString().orElse(null);
		context.sender.ban(BanQuerySpec.builder()
				.deleteMessageDays(deleteMessages ? 1 : 0)
				.reason(reason)
				.build()
		).block();
	};
}
