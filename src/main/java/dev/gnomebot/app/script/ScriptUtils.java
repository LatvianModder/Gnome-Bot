package dev.gnomebot.app.script;

import dev.gnomebot.app.util.Utils;
import discord4j.core.object.reaction.ReactionEmoji;

public class ScriptUtils {
	public static String reactionToString(ReactionEmoji emoji) {
		return Utils.reactionToString(emoji);
	}

	public static ReactionEmoji stringToReaction(String s) {
		return Utils.stringToReaction(s);
	}
}
