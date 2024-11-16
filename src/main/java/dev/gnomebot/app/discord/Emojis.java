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

	ReactionEmoji.Custom YES = custom(1295675217863250023L, "Yes", false);
	ReactionEmoji.Custom NO = custom(1295675226037682207L, "No", false);

	static ReactionEmoji.Custom yesNo(boolean yes) {
		return yes ? YES : NO;
	}

	ReactionEmoji.Unicode POLICE_CAR = unicode("🚓");
	ReactionEmoji.Unicode NO_ENTRY = unicode("🚫");
	ReactionEmoji.Unicode STAR = unicode("⭐");
	ReactionEmoji.Unicode BOOT = unicode("\uD83D\uDC62");
	ReactionEmoji.Custom VOTENONE = custom(1295674246898516008L, "RemoveVote", false);
	ReactionEmoji.Custom VOTEUP = custom(1295674258202300468L, "Upvote", false);
	ReactionEmoji.Custom VOTEDOWN = custom(1295674268121567292L, "Downvote", false);
	ReactionEmoji.Custom DOWNLOAD = custom(1295682331016826900L, "Download", false);
	ReactionEmoji.Custom GNOME_PING = custom(1295673389284982784L, "Pinged", false);
	ReactionEmoji.Custom GNOME_HAHA_YES = custom(1295673202768478261L, "GnomeYes", false);
	ReactionEmoji.Custom GNOME_HAHA_NO = custom(1295673187543023679L, "GnomeNo", false);
	ReactionEmoji.Custom GNOME_SHERLOCK = custom(1295672789176418394L, "SherlockGnomes", false);
	ReactionEmoji.Unicode RAGE = unicode("\uD83D\uDE21");
	ReactionEmoji.Unicode STOP = unicode("\uD83D\uDED1");
	ReactionEmoji.Unicode REFRESH = unicode("\uD83D\uDD04");
	ReactionEmoji.Custom ALERT = custom(1295672502961438822L, "Alert", true);
	ReactionEmoji.Custom WARNING = custom(1295672318416388127L, "Warning", false);
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
