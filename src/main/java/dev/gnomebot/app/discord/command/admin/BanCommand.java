package dev.gnomebot.app.discord.command.admin;

import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.MemberHandler;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.data.Confirm;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.BanQuerySpec;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Permission;

import java.util.Collections;

public class BanCommand extends ApplicationCommands {
	public static void run(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		event.context.checkGlobalPerms(Permission.BAN_MEMBERS);

		var user = event.get("user").asUser().orElse(null);
		Member member = null;

		try {
			member = user == null ? null : user.asMember(SnowFlake.convert(event.context.gc.guildId)).block();
		} catch (Exception ex) {
		}

		var reason0 = event.get("reason").asString();
		var reason = reason0.isEmpty() ? "Not specified" : reason0;
		var deleteMessages = event.get("delete-messages").asBoolean(false);

		if (user == null) {
			throw error("User not found!");
		} else if (user.isBot() || member != null && event.context.gc.getAuthLevel(member).is(AuthLevel.ADMIN)) {
			throw error("Nice try.");
		}

		event.context.allowedMentions = AllowedMentions.builder().allowUser(user.getId()).allowUser(event.context.sender.getId()).build();
		event.context.reply(event.context.sender.getMention() + " banned " + user.getMention());

		var dm = DM.send(event.context.handler, user.getUserData(), "You've been banned from " + event.context.gc + ", reason: " + reason, false).isPresent();

		if (member != null) {
			MemberHandler.ignoreNextBan = true;
		}

		event.context.gc.getGuild().ban(user.getId(), BanQuerySpec.builder()
				.reason(reason)
				.deleteMessageDays(deleteMessages ? 1 : null)
				.build()
		).subscribe();

		event.context.gc.adminLogChannelEmbed(user.getUserData(), event.context.gc.adminLogChannel, spec -> {
			spec.description("Bye " + user.getMention());
			spec.author(user.getTag() + " was banned", user.getAvatarUrl());
			spec.inlineField("Reason", reason);
			spec.inlineField("DM successful", dm ? "Yes" : "No");
			spec.inlineField("Messages deleted", deleteMessages ? "Yes" : "No");
			spec.footer(event.context.sender.getUsername(), event.context.sender.getAvatarUrl());
		});

		event.context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.BAN)
				.user(user)
				.source(event.context.sender)
				.content(reason)
				.flags(GnomeAuditLogEntry.Flags.DM, dm)
				.flags(GnomeAuditLogEntry.Flags.DELETED_MESSAGES, deleteMessages)
		);

		// m.addReaction(DiscordHandler.EMOJI_COMMAND_ERROR).block();
		// ReactionHandler.addListener();

		event.respond("Banned! DM successful: " + dm);
	}

	public static void banButtonCallback(ComponentEventWrapper event, long other, String reason, Confirm confirm) {
		event.context.checkSenderAdmin();
		event.context.gc.getGuild().ban(SnowFlake.convert(other), BanQuerySpec.builder().deleteMessageDays(1).reason(reason).build()).subscribe();
		Utils.editComponents(event.event.getMessage().orElse(null), Collections.singletonList(ActionRow.of(Button.danger("none", Emojis.WARNING, "Banned by " + event.context.sender.getUsername() + "!")).getData()));
		event.respond("Banned <@" + other + ">");
	}
}
