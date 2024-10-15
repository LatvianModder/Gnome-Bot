package dev.gnomebot.app.discord;

import com.mongodb.BasicDBList;
import com.mongodb.client.model.Updates;
import dev.gnomebot.app.BrainEventType;
import dev.gnomebot.app.data.DiscordMember;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.webutils.TimeUtils;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.guild.UnbanEvent;
import discord4j.core.object.audit.ActionType;
import discord4j.core.object.audit.AuditLogEntry;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.spec.AuditLogQuerySpec;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;

public class MemberHandler {
	public static final int ACTION_JOINED = 1;
	public static final int ACTION_LEFT = 2;
	public static final int ACTION_BANNED = 3;
	public static final int ACTION_MESSAGE = 4;
	public static final int ACTION_IMPORT_DATA = 5;
	public static final int ACTION_UNBANNED = 6;

	public static boolean ignoreNextBan = false;

	public static final HashSet<Long> lockdownKicks = new HashSet<>();

	public static void updateMember(GuildCollections gc, User user, @Nullable Member member, int action, @Nullable DiscordMember discordMember, @Nullable DiscordMessage discordMessage) {
		var memberUpdates = new ArrayList<Bson>();
		memberUpdates.add(Updates.set("name", user.getUsername()));
		memberUpdates.add(Updates.set("avatar", user.getAvatarUrl()));
		memberUpdates.add(Updates.set("discriminator", user.getDiscriminator()));

		var roleList = new BasicDBList();
		var memberFlags = 0L;

		if (user.isBot()) {
			memberFlags |= DiscordMember.FLAG_BOT;
		}

		if (user.hasAnimatedAvatar()) {
			memberFlags |= DiscordMember.FLAG_ANIMATED_AVATAR;
		}

		if (member != null) {
			if (member.getNickname().isPresent()) {
				memberUpdates.add(Updates.set("nickname", member.getNickname().get()));
				memberFlags |= DiscordMember.FLAG_NICKNAME;
			} else {
				memberUpdates.add(Updates.unset("nickname"));
			}

			for (var role : member.getRoleIds()) {
				roleList.add(role.asLong());
			}
		} else {
			memberUpdates.add(Updates.unset("nickname"));
			memberFlags |= DiscordMember.FLAG_GONE;
		}

		memberUpdates.add(Updates.set("roles", roleList));
		memberUpdates.add(Updates.set("flags", memberFlags));

		gc.members.query(user.getId().asLong()).upsert(memberUpdates);
	}

	public static void joined(DiscordHandler handler, GuildCollections gc, MemberJoinEvent event) {
		var member = event.getMember();
		var nowSecond = Instant.now().getEpochSecond();

		var accountAge = nowSecond - member.getId().getTimestamp().getEpochSecond();

		if (accountAge <= 604800L && gc.logNewAccountsChannel.isSet()) {
			var sb = member.getMention() +
					" (" +
					member.getTag() +
					") is a new account: " +
					Utils.formatRelativeDate(member.getId().getTimestamp());
			gc.logNewAccountsChannel.messageChannel().ifPresent(c -> c.createMessage(sb).subscribe());
		}

		gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.JOIN)
				.user(member)
				.flags(GnomeAuditLogEntry.Flags.LOCKDOWN, gc.lockdownMode.get())
				.extra("account_age", accountAge)
		);

		if (gc.lockdownMode.get()) {
			if (gc.logLeavingChannel.isSet()) {
				lockdownKicks.add(member.getId().asLong());
			}

			member.kick("Lockdown Mode").subscribe();
			return;
		} else if (gc.kickNewAccounts.get() > 0 && accountAge < gc.kickNewAccounts.get()) {
			if (gc.logLeavingChannel.isSet()) {
				lockdownKicks.add(member.getId().asLong());
			}

			DM.send(handler, member.getUserData(), "Your account is too new! You can return " + Utils.formatRelativeDate(Instant.ofEpochSecond(member.getId().getTimestamp().getEpochSecond() + gc.kickNewAccounts.get())), false);
			member.kick("New Account").subscribe();
			return;
		}

		gc.pushRecentUser(member.getId().asLong(), member.getTag());

		var prevMessages = 0L;
		var mdoc = gc.members.query(member.getId().asLong()).firstDocument();

		if (mdoc != null) {
			prevMessages = mdoc.containsKey("total_messages") ? mdoc.getLong("total_messages") : 0L;
		}

		if (gc.regularRole.isSet() && gc.regularMessages.get() > 0 && prevMessages >= gc.regularMessages.get()) {
			gc.regularRole.role().ifPresent(r -> r.add(member, "Reached Regular"));
		}

		// App.info(Utils.ANSI_GREEN + member.getTag() + Utils.ANSI_RESET + " has joined the " + Utils.ANSI_CYAN + gc + Utils.ANSI_RESET + " server!");
		BrainEventType.MEMBER_JOINED.build(gc.guildId).post();

		var oldMember = gc.members.findFirst(member);

		if (oldMember != null) {
			for (var l : oldMember.getRoles()) {
				try {
					member.addRole(SnowFlake.convert(l)).block();
				} catch (Exception ex) {
				}
			}
		}

		try {
			updateMember(gc, member, member, ACTION_JOINED, oldMember, null);
		} catch (Exception ex) {
			Log.error("Failed to save member from joining: " + ex);
		}
	}

	public static void left(DiscordHandler handler, GuildCollections gc, MemberLeaveEvent event) {
		var member = event.getMember().orElse(null);
		var mems = member == null || member.getJoinTime().isEmpty() ? 0L : Instant.now().getEpochSecond() - member.getJoinTime().get().getEpochSecond();

		gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.LEAVE)
				.user(event.getUser())
				.content(TimeUtils.prettyTimeString(mems))
				.flags(GnomeAuditLogEntry.Flags.LOCKDOWN, gc.lockdownMode.get())
				.extra("member_for", mems)
		);

		// App.info(Utils.ANSI_GREEN + event.getUser().getUsername() + Utils.ANSI_RESET + " has left the " + Utils.ANSI_CYAN + gc + Utils.ANSI_RESET + " server! (" + Utils.prettyTimeString(mems) + " of membership)");
		BrainEventType.MEMBER_LEFT.build(gc.guildId).post();

		if (gc.logLeavingChannel.isSet()) {
			var sb = new StringBuilder();
			sb.append(event.getUser().getMention());
			sb.append(" (");
			sb.append(event.getUser().getTag());

			if (member != null && member.getNickname().isPresent()) {
				sb.append(" / ");
				sb.append(member.getNickname().get());
			}

			if (lockdownKicks.remove(event.getUser().getId().asLong())) {
				sb.append(") was kicked by lockdown after ");
			} else {
				sb.append(") has left the server after ");
			}

			sb.append(TimeUtils.prettyTimeString(mems));
			sb.append(" of membership");

			gc.logLeavingChannel.messageChannel().ifPresent(c -> c.createMessage(sb.toString()).subscribe());
		}

		updateMember(gc, event.getUser(), member, ACTION_LEFT, gc.members.findFirst(event.getUser()), null);
	}

	public static void banned(DiscordHandler handler, BanEvent event) {
		if (ignoreNextBan) {
			ignoreNextBan = false;
			return;
		}

		var gc = handler.app.db.guild(event.getGuildId());
		updateMember(gc, event.getUser(), null, ACTION_BANNED, gc.members.findFirst(event.getUser()), null);

		if (gc.adminLogChannel.isSet()) {
			AuditLogEntry entry = null;

			try {
				Thread.sleep(3000L);
				entry = findBanEntry(gc, event.getUser().getId().asLong());
			} catch (Exception ex) {
			}

			var responsible = entry == null || entry.getResponsibleUserId().isEmpty() ? null : handler.getUser(entry.getResponsibleUserId().get().asLong());
			var reason = entry == null ? "Not specified" : entry.getReason().orElse("Not specified");

			// App.info(Utils.ANSI_RED + event.getUser().getUsername() + Utils.ANSI_RESET + " was banned from " + Utils.ANSI_CYAN + gc + Utils.ANSI_RESET + " server by " + (responsible == null ? "Unknown" : responsible.getTag()) + "! Reason: " + reason);
			BrainEventType.MEMBER_BANNED.build(gc.guildId).post();

			gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.BAN)
					.user(event.getUser())
					.source(responsible)
					.content(reason)
			);

			if (responsible != null && responsible.isBot()) {
				gc.logLeavingChannel.messageChannel().ifPresent(c -> c.createMessage(event.getUser().getMention() + " (" + event.getUser().getTag() + ") was banned by " + responsible.getMention() + ": " + reason).subscribe());
			} else {
				gc.adminLogChannelEmbed(event.getUser().getUserData(), gc.adminLogChannel, spec -> {
					spec.description("Bye " + event.getUser().getMention());
					spec.timestamp(Instant.now());
					spec.author(event.getUser().getTag() + " was banned", event.getUser().getAvatarUrl());
					spec.field("Reason", reason);

					if (responsible == null) {
						spec.footer("Unknown", null);
					} else {
						spec.footer(responsible.getUsername(), responsible.getAvatarUrl());
					}
				});
			}
		}
	}

	@Nullable
	private static AuditLogEntry findBanEntry(GuildCollections gc, long target) throws Exception {
		for (var part : gc.getGuild().getAuditLog(AuditLogQuerySpec.builder()
				.actionType(ActionType.MEMBER_BAN_ADD)
				.build()
		).take(10L, true).toIterable()) {
			for (var entry : part.getEntries()) {
				if (entry.getTargetId().isPresent() && entry.getTargetId().get().asLong() == target) {
					return entry;
				}
			}
		}

		return null;
	}

	public static void unbanned(DiscordHandler handler, UnbanEvent event) {
		var gc = handler.app.db.guild(event.getGuildId());
		updateMember(gc, event.getUser(), null, ACTION_UNBANNED, gc.members.findFirst(event.getUser()), null);

		gc.adminLogChannelEmbed(event.getUser().getUserData(), gc.adminLogChannel, spec -> {
			spec.color(EmbedColor.GREEN);
			spec.description("Welcome back " + event.getUser().getMention());
			spec.timestamp(Instant.now());
			spec.author(event.getUser().getTag() + " was unbanned", event.getUser().getAvatarUrl());
		});
	}
}
