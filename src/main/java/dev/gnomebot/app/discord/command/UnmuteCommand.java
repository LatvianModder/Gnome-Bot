package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.EmbedBuilder;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;

/**
 * @author LatvianModder
 */
public class UnmuteCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("unmute")
			.description("Unmutes a member")
			.add(user("user").required())
			.run(UnmuteCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) {
		if (!event.context.gc.mutedRole.isSet()) {
			throw new GnomeException("Muted role not set!");
		}

		event.context.checkBotPerms(Permission.BAN_MEMBERS);
		event.context.checkSenderPerms(Permission.BAN_MEMBERS);

		User user = event.get("user").asUser().orElse(null);

		if (user == null) {
			throw new GnomeException("User not found!");
		} else if (user.isBot() || event.context.gc.getAuthLevel(user.getId()).is(AuthLevel.ADMIN)) {
			throw new GnomeException("Nice try.");
		}

		event.acknowledge();
		event.context.gc.unmute(user.getId(), 0L);
		event.respond(EmbedBuilder.create().color(EmbedColor.GREEN).author(user.getTag() + " has been unmuted!", user.getAvatarUrl()));
	}
}
