package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Permission;

/**
 * @author LatvianModder
 */
public class WarnCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("warn")
			.description("Warns a member")
			.add(user("user").required())
			.add(string("reason"))
			.run(WarnCommand::run);

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

		if (user == null) {
			throw error("User not found!");
		} else if (user.isBot() || member != null && event.context.gc.getAuthLevel(member).is(AuthLevel.ADMIN)) {
			throw error("Nice try.");
		}

		event.context.allowedMentions = AllowedMentions.builder().allowUser(user.getId()).allowUser(event.context.sender.getId()).build();
		boolean dm = DM.send(event.context.handler, user, "You've been warned on " + event.context.gc + ", reason: " + reason, true).isPresent();

		if (dm) {
			event.context.reply(event.context.sender.getMention() + " warned " + user.getMention());
		} else {
			event.context.reply(event.context.sender.getMention() + " warned " + user.getMention() + ": " + reason);
		}

		event.respond("Warned! DM successful: " + dm);

		// event.gc.getGuild().kick(user.getId(), reason).subscribe();

		event.context.gc.adminLogChannelEmbed(event.context.gc.adminLogChannel, spec -> {
			spec.description("Bad " + user.getMention());
			spec.author(user.getTag() + " was warned", user.getAvatarUrl());
			spec.inlineField("Reason", reason);
			spec.inlineField("DM successful", dm ? "Yes" : "No");
			spec.footer(event.context.sender.getUsername(), event.context.sender.getAvatarUrl());
		});

		event.context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.WARN)
				.user(user)
				.source(event.context.sender)
				.content(reason)
				.extra("dm", dm)
		);

		// m.addReaction(DiscordHandler.EMOJI_COMMAND_ERROR).block();
		// ReactionHandler.addListener();
	}
}
