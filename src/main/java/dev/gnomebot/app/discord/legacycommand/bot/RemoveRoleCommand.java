package dev.gnomebot.app.discord.legacycommand.bot;

import dev.gnomebot.app.discord.legacycommand.CommandCallback;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.discord.legacycommand.LegacyDiscordCommand;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.common.util.Snowflake;

/**
 * @author LatvianModder
 */
public class RemoveRoleCommand {
	@LegacyDiscordCommand(name = "remove_role", arguments = "<role>", permissionLevel = AuthLevel.BOT)
	public static final CommandCallback COMMAND = (context, reader) -> {
		Snowflake id = reader.readRole().orElseThrow(() -> new DiscordCommandException("Role not found!")).id;

		if (context.sender.getRoleIds().contains(id)) {
			context.sender.removeRole(id).block();
		}
	};
}
