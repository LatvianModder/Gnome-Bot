package dev.gnomebot.app;

import dev.gnomebot.app.data.GuildCollections;
import dev.latvian.apps.ansi.ANSI;
import discord4j.core.object.entity.Guild;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public record BrainEventType(String name, String symbol, Function<Object, ANSI> color) {
	public static final BrainEventType MESSAGE_CREATED_NO_ROLE = new BrainEventType("message_created_no_role", "■", ANSI::lightGray);
	public static final BrainEventType MESSAGE_CREATED_ANY_ROLE = new BrainEventType("message_created_any_role", "■", ANSI::yellow);
	public static final BrainEventType UNKNOWN_MESSAGE = new BrainEventType("unknown_message", "■", ANSI::teal);
	public static final BrainEventType MESSAGE_CREATED_ADMIN = new BrainEventType("message_created_admin", "■", ANSI::purple);
	public static final BrainEventType MESSAGE_CREATED_BOT = new BrainEventType("message_created_bot", "■", ANSI::green);
	public static final BrainEventType MESSAGE_EDITED = new BrainEventType("message_edited", "■", ANSI::orange);
	public static final BrainEventType MESSAGE_DELETED = new BrainEventType("message_deleted", "■", ANSI::red);
	public static final BrainEventType SUSPICIOUS_MESSAGE = new BrainEventType("suspicious_message", "■", ANSI::darkRed);
	public static final BrainEventType COMMAND_SUCCESS = new BrainEventType("command_success", "◆", ANSI::blue);
	public static final BrainEventType COMMAND_FAIL = new BrainEventType("command_fail", "◆", ANSI::red);
	public static final BrainEventType REACTION_ADDED = new BrainEventType("reaction_added", "\uD83D\uDDF8", ANSI::green); // 🗸
	public static final BrainEventType REACTION_REMOVED = new BrainEventType("reaction_removed", "\uD83D\uDDF8", ANSI::red); // 🗸
	public static final BrainEventType VOICE_JOINED = new BrainEventType("voice_joined", "♪", ANSI::green);
	public static final BrainEventType VOICE_LEFT = new BrainEventType("voice_left", "♪", ANSI::red);
	public static final BrainEventType VOICE_CHANGED = new BrainEventType("voice_changed", "♪", ANSI::yellow);
	public static final BrainEventType REFRESHED_GUILD_CACHE = new BrainEventType("refreshed_guild_cache", "\uD83D\uDE7E", ANSI::lightGray); // 🙾
	public static final BrainEventType REFRESHED_CHANNEL_CACHE = new BrainEventType("refreshed_channel_cache", "\uD83D\uDE7E", ANSI::magenta); // 🙾
	public static final BrainEventType REFRESHED_PINGS = new BrainEventType("refreshed_pings", "\uD83D\uDE7E", ANSI::green); // 🙾
	public static final BrainEventType REFRESHED_ROLE_CACHE = new BrainEventType("refreshed_role_cache", "\uD83D\uDE7E", ANSI::yellow); // 🙾
	public static final BrainEventType MEMBER_JOINED = new BrainEventType("member_joined", "⬤", ANSI::blue);
	public static final BrainEventType MEMBER_LEFT = new BrainEventType("member_left", "⬤", ANSI::red);
	public static final BrainEventType MEMBER_MUTED = new BrainEventType("member_muted", "☠", ANSI::red);
	public static final BrainEventType MEMBER_BANNED = new BrainEventType("member_banned", "☠", ANSI::darkRed);
	public static final BrainEventType WEB_REQUEST = new BrainEventType("web_request", "◆", ANSI::cyan);
	public static final BrainEventType PRESENCE_UPDATED = new BrainEventType("presence_updated", "◆", ANSI::lightGray);
	public static final BrainEventType AUDIT_LOG = new BrainEventType("audit_log", "◆", ANSI::yellow);

	public BrainEvent build(long guild) {
		return new BrainEvent(this, guild);
	}

	public BrainEvent build(GuildCollections gc) {
		return build(gc.guildId);
	}

	public BrainEvent build(@Nullable Guild guild) {
		return build(guild == null ? 0L : guild.getId().asLong());
	}
}
