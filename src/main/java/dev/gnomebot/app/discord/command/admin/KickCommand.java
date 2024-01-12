package dev.gnomebot.app.discord.command.admin;

import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.data.Confirm;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.rest.util.Permission;

import java.util.Collections;

public class KickCommand extends ApplicationCommands {
	public static void run(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		event.context.checkGlobalPerms(Permission.KICK_MEMBERS);

		var user = event.get("user").asUser().orElse(null);
		Member member = null;

		try {
			member = user == null ? null : user.asMember(SnowFlake.convert(event.context.gc.guildId)).block();
		} catch (Exception ex) {
		}

		var reason0 = event.get("reason").asString();
		var reason = reason0.isEmpty() ? "Not specified" : reason0;

		if (user == null) {
			throw error("User not found!");
		} else if (user.isBot() || member != null && event.context.gc.getAuthLevel(member).is(AuthLevel.ADMIN)) {
			throw error("Nice try.");
		}

		event.context.reply(event.context.sender.getMention() + " kicked " + user.getMention());

		var dm = DM.send(event.context.handler, user, "You've been kicked from " + event.context.gc + ", reason: " + reason, true).isPresent();

		if (member != null) {
			// MemberHandler.ignoreNextBan = true;
		}

		event.context.gc.getGuild().kick(user.getId(), reason).subscribe();

		event.context.gc.adminLogChannelEmbed(user.getUserData(), event.context.gc.adminLogChannel, spec -> {
			spec.description("Bye " + user.getMention());
			spec.author(user.getTag() + " was kicked", user.getAvatarUrl());
			spec.inlineField("Reason", reason);
			spec.inlineField("DM successful", dm ? "Yes" : "No");
			spec.footer(event.context.sender.getUsername(), event.context.sender.getAvatarUrl());
		});

		event.context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.KICK)
				.user(user)
				.source(event.context.sender)
				.content(reason)
				.flags(GnomeAuditLogEntry.Flags.DM, dm)
		);

		// m.addReaction(DiscordHandler.EMOJI_COMMAND_ERROR).block();
		// ReactionHandler.addListener();

		event.respond("Kicked! DM successful: " + dm);
	}

	public static void kickButtonCallback(ComponentEventWrapper event, long other, String reason, Confirm confirm) {
		event.context.checkSenderAdmin();
		event.context.gc.getGuild().kick(SnowFlake.convert(other), reason).subscribe();
		Utils.editComponents(event.event.getMessage().orElse(null), Collections.singletonList(ActionRow.of(Button.danger("none", Emojis.WARNING, "Kicked by " + event.context.sender.getUsername() + "!")).getData()));
		event.respond("Kicked <@" + other + ">");
	}
}
