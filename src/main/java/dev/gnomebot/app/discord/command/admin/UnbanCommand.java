package dev.gnomebot.app.discord.command.admin;

import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import discord4j.core.object.entity.Member;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Permission;

public class UnbanCommand extends ApplicationCommands {
	public static void run(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		event.context.checkGlobalPerms(Permission.BAN_MEMBERS);

		var user = event.get("user").asUser().orElse(null);

		Member member = null;

		try {
			member = user == null ? null : user.asMember(event.context.gc.guildId).block();
		} catch (Exception ex) {
		}

		if (user == null) {
			throw error("User not found!");
		} else if (user.isBot()) {
			throw error("Nice try.");
		} else if (member != null) {
			throw error("User isn't banned!");
		}

		event.context.allowedMentions = AllowedMentions.builder().allowUser(user.getId()).allowUser(event.context.sender.getId()).build();
		event.context.gc.getGuild().unban(user.getId()).block();
		event.respond(event.context.sender.getMention() + " unbanned " + user.getMention());
	}
}
