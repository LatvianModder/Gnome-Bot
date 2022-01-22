package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.discord.EmbedColors;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.core.object.entity.User;

/**
 * @author LatvianModder
 */
public class UnmuteCommand {
	@LegacyDiscordCommand(name = "unmute", help = "Unmutes a member", arguments = "<member>", permissionLevel = AuthLevel.ADMIN)
	public static final CommandCallback COMMAND = (context, reader) -> {
		if (!context.gc.mutedRole.isSet()) {
			throw new DiscordCommandException("Muted role not set!");
		}

		User m = reader.readUser().orElseThrow(() -> new DiscordCommandException("User not found!"));
		context.gc.unmute(m.getId(), 0L);
		context.reply(spec -> {
			spec.color(EmbedColors.GREEN);
			spec.author(m.getTag() + " has been unmuted!", null, m.getAvatarUrl());
		});
	};
}
