package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import dev.gnomebot.app.discord.Confirm;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.MemberHandler;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.spec.BanQuerySpec;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Permission;

import java.util.Collections;

/**
 * @author LatvianModder
 */
public class BanCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("ban")
			.description("Bans a member")
			.add(user("user").required())
			.add(string("reason"))
			.add(bool("delete_messages").description("Deletes Messages"))
			.run(BanCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();
		event.context.checkBotPerms(Permission.BAN_MEMBERS);
		event.context.checkSenderPerms(Permission.BAN_MEMBERS);

		User user = event.get("user").asUser().orElse(null);
		Member member = null;

		try {
			member = user == null ? null : user.asMember(event.context.gc.guildId).block();
		} catch (Exception ex) {
		}

		String reason0 = event.get("reason").asString();
		String reason = reason0.isEmpty() ? "Not specified" : reason0;
		boolean deleteMessages = event.get("delete_messages").asBoolean(false);

		if (user == null) {
			throw error("User not found!");
		} else if (user.isBot() || member != null && event.context.gc.getAuthLevel(member).is(AuthLevel.ADMIN)) {
			throw error("Nice try.");
		}

		event.context.allowedMentions = AllowedMentions.builder().allowUser(user.getId()).allowUser(event.context.sender.getId()).build();
		event.context.reply(event.context.sender.getMention() + " banned " + user.getMention());

		boolean dm = DM.send(event.context.handler, user, "You've been banned from " + event.context.gc + ", reason: " + reason, false).isPresent();

		if (member != null) {
			MemberHandler.ignoreNextBan = true;
		}

		App.LOGGER.memberBanned();

		event.context.gc.getGuild().ban(user.getId(), BanQuerySpec.builder()
				.reason(reason)
				.deleteMessageDays(deleteMessages ? 1 : null)
				.build()
		).subscribe();

		event.context.gc.adminLogChannelEmbed(event.context.gc.adminLogChannel, spec -> {
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
				.extra("dm", dm)
				.extra("delete_messages", deleteMessages)
		);

		// m.addReaction(DiscordHandler.EMOJI_COMMAND_ERROR).block();
		// ReactionHandler.addListener();

		event.respond("Banned! DM successful: " + dm);
	}

	public static void banButtonCallback(ComponentEventWrapper event, Snowflake other, String reason, Confirm confirm) {
		event.context.checkSenderAdmin();
		event.context.gc.getGuild().ban(other, BanQuerySpec.builder().deleteMessageDays(1).reason(reason).build()).subscribe();
		Utils.editComponents(event.event.getMessage().orElse(null), Collections.singletonList(ActionRow.of(Button.danger("none", Emojis.WARNING, "Banned by " + event.context.sender.getUsername() + "!")).getData()));
		event.respond("Banned <@" + other.asString() + ">");
	}
}
