package dev.gnomebot.app.discord;

import discord4j.common.util.Snowflake;
import discord4j.core.object.reaction.ReactionEmoji;

import java.util.regex.Pattern;

public interface Emojis {
	Pattern CUSTOM_REGEX = Pattern.compile("<a?:\\w+:\\d+>");

	ReactionEmoji.Unicode POLICE_CAR = ReactionEmoji.unicode("🚓");
	ReactionEmoji.Unicode NO_ENTRY = ReactionEmoji.unicode("🚫");
	ReactionEmoji.Unicode STAR = ReactionEmoji.unicode("⭐");
	ReactionEmoji.Unicode BOOT = ReactionEmoji.unicode("\uD83D\uDC62");
	ReactionEmoji.Unicode WARNING = ReactionEmoji.unicode("⚠️");
	ReactionEmoji.Unicode DOOR = ReactionEmoji.unicode("\uD83D\uDEAA");
	ReactionEmoji.Custom VOTENONE = ReactionEmoji.custom(Snowflake.of(873933804383928411L), "votenone", false);
	ReactionEmoji.Custom VOTEUP = ReactionEmoji.custom(Snowflake.of(873933822381678612L), "voteup", false);
	ReactionEmoji.Custom VOTEDOWN = ReactionEmoji.custom(Snowflake.of(873933788751753237L), "votedown", false);
	ReactionEmoji.Custom DOWNLOAD = ReactionEmoji.custom(Snowflake.of(873934864217419837L), "download", false);
	ReactionEmoji.Unicode CHECKMARK = ReactionEmoji.unicode("✅");
	ReactionEmoji.Custom GNOME_PING = ReactionEmoji.custom(Snowflake.of(873937057553215538L), "GnomePing", false);
	ReactionEmoji.Custom GNOME_HAHA_YES = ReactionEmoji.custom(Snowflake.of(720018305963917383L), "GnomeHahaYes", false);
	ReactionEmoji.Custom GNOME_HAHA_NO = ReactionEmoji.custom(Snowflake.of(736853401131810826L), "GnomeHahaNo", false);
	ReactionEmoji.Custom GNOME_SHERLOCK = ReactionEmoji.custom(Snowflake.of(750360232357658668L), "SherlockGnome", false);
	ReactionEmoji.Unicode RAGE = ReactionEmoji.unicode("\uD83D\uDE21");
	ReactionEmoji.Unicode STOP = ReactionEmoji.unicode("\uD83D\uDED1");
	ReactionEmoji.Unicode REFRESH = ReactionEmoji.unicode("\uD83D\uDD04");
	ReactionEmoji.Custom ALERT = ReactionEmoji.custom(Snowflake.of(660111101605969940L), "alert", true);
	ReactionEmoji.Unicode PENCIL = ReactionEmoji.unicode("\uD83D\uDCDD");

	ReactionEmoji.Unicode[] NUMBERS = {
			ReactionEmoji.unicode("1\uFE0F\u20E3"),
			ReactionEmoji.unicode("2\uFE0F\u20E3"),
			ReactionEmoji.unicode("3\uFE0F\u20E3"),
			ReactionEmoji.unicode("4\uFE0F\u20E3"),
			ReactionEmoji.unicode("5\uFE0F\u20E3"),
			ReactionEmoji.unicode("6\uFE0F\u20E3"),
			ReactionEmoji.unicode("7\uFE0F\u20E3"),
			ReactionEmoji.unicode("8\uFE0F\u20E3"),
			ReactionEmoji.unicode("9\uFE0F\u20E3"),
			ReactionEmoji.unicode("\uD83D\uDD1F"),
	};

	String[] NUMBER_STRINGS = {
			":one:",
			":two:",
			":three:",
			":four:",
			":five:",
			":six:",
			":seven:",
			":eight:",
			":nine:",
			":keycap_ten:",
	};

	static String stripEmojis(String content) {
		return CUSTOM_REGEX.matcher(content).replaceAll("").trim();
	}
}
