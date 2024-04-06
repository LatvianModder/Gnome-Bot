package dev.gnomebot.app.discord;

import dev.gnomebot.app.util.SnowFlake;
import discord4j.core.object.reaction.ReactionEmoji;

import java.util.regex.Pattern;

public interface Emojis {
	Pattern GUILD_EMOJI_PATTERN = Pattern.compile("<a?:\\w+:\\d+>");
	Pattern GUILD_EMOJI_PATTERN_GROUPS = Pattern.compile("<a?:(\\w+):(\\d+)>");

	static ReactionEmoji.Custom custom(long id, String name, boolean animated) {
		return ReactionEmoji.custom(SnowFlake.convert(id), name, animated);
	}

	static ReactionEmoji.Unicode unicode(String name) {
		return ReactionEmoji.unicode(name);
	}

	ReactionEmoji.Custom YES = custom(1183755303079125002L, "yes", false);
	ReactionEmoji.Custom NO = custom(1183755305037869157L, "no", false);

	static ReactionEmoji.Custom yesNo(boolean yes) {
		return yes ? YES : NO;
	}

	ReactionEmoji.Unicode POLICE_CAR = unicode("🚓");
	ReactionEmoji.Unicode NO_ENTRY = unicode("🚫");
	ReactionEmoji.Unicode STAR = unicode("⭐");
	ReactionEmoji.Unicode BOOT = unicode("\uD83D\uDC62");
	ReactionEmoji.Unicode WARNING = unicode("⚠️");
	ReactionEmoji.Unicode DOOR = unicode("\uD83D\uDEAA");
	ReactionEmoji.Custom VOTENONE = custom(873933804383928411L, "votenone", false);
	ReactionEmoji.Custom VOTEUP = custom(873933822381678612L, "voteup", false);
	ReactionEmoji.Custom VOTEDOWN = custom(873933788751753237L, "votedown", false);
	ReactionEmoji.Custom DOWNLOAD = custom(873934864217419837L, "download", false);
	ReactionEmoji.Unicode CHECKMARK = unicode("✅");
	ReactionEmoji.Custom GNOME_PING = custom(873937057553215538L, "GnomePing", false);
	ReactionEmoji.Custom GNOME_HAHA_YES = custom(720018305963917383L, "GnomeHahaYes", false);
	ReactionEmoji.Custom GNOME_HAHA_NO = custom(736853401131810826L, "GnomeHahaNo", false);
	ReactionEmoji.Custom GNOME_SHERLOCK = custom(750360232357658668L, "SherlockGnome", false);
	ReactionEmoji.Unicode RAGE = unicode("\uD83D\uDE21");
	ReactionEmoji.Unicode STOP = unicode("\uD83D\uDED1");
	ReactionEmoji.Unicode REFRESH = unicode("\uD83D\uDD04");
	ReactionEmoji.Custom ALERT = custom(660111101605969940L, "alert", true);
	ReactionEmoji.Unicode PENCIL = unicode("\uD83D\uDCDD");

	ReactionEmoji.Unicode[] NUMBERS = {
			ReactionEmoji.unicode("1️⃣"),
			ReactionEmoji.unicode("2️⃣"),
			ReactionEmoji.unicode("3️⃣"),
			ReactionEmoji.unicode("4️⃣"),
			ReactionEmoji.unicode("5️⃣"),
			ReactionEmoji.unicode("6️⃣"),
			ReactionEmoji.unicode("7️⃣"),
			ReactionEmoji.unicode("8️⃣"),
			ReactionEmoji.unicode("9️⃣"),
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
		return GUILD_EMOJI_PATTERN.matcher(content).replaceAll("").trim();
	}
}
