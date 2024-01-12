package dev.gnomebot.app.discord.command.admin;

import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.rest.util.Permission;

import java.util.Collections;

public class UnmuteCommand extends ApplicationCommands {
	public static void run(ChatInputInteractionEventWrapper event) {
		if (!event.context.gc.mutedRole.isSet()) {
			throw new GnomeException("Muted role not set!");
		}

		event.context.checkGlobalPerms(Permission.BAN_MEMBERS);

		var user = event.get("user").asUser().orElse(null);

		if (user == null) {
			throw new GnomeException("User not found!");
		} else if (user.isBot() || event.context.gc.getAuthLevel(user.getId().asLong()).is(AuthLevel.ADMIN)) {
			throw new GnomeException("Nice try.");
		}

		event.acknowledge();
		event.context.gc.unmute(user.getId().asLong(), 0L, "");
		event.respond(EmbedBuilder.create().color(EmbedColor.GREEN).author(user.getTag() + " has been unmuted!", user.getAvatarUrl()));
	}

	public static void unmuteButtonCallback(ComponentEventWrapper event, long other) {
		event.context.checkSenderAdmin();
		event.context.gc.unmute(other, 0L, "");
		Utils.editComponents(event.event.getMessage().orElse(null), Collections.singletonList(ActionRow.of(Button.secondary("none", Emojis.CHECKMARK, "Unmuted by " + event.context.sender.getUsername() + "!")).getData()));
		event.respond("Unmuted <@" + other + ">");
	}
}
