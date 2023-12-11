package dev.gnomebot.app.data;

import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.reaction.ReactionEmoji;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public enum Vote {
	NONE(null, Emojis.VOTENONE),
	UP(Boolean.TRUE, Emojis.VOTEUP),
	DOWN(Boolean.FALSE, Emojis.VOTEDOWN);

	public static final Vote[] VALUES = values();

	public final Boolean value;
	public final ReactionEmoji reaction;

	Vote(@Nullable Boolean v, ReactionEmoji r) {
		value = v;
		reaction = r;
	}

	@Override
	public String toString() {
		return Utils.reactionToString(reaction);
	}

	public static Vote fromBoolean(@Nullable Boolean b) {
		return b == null ? NONE : b ? UP : DOWN;
	}

	public static Optional<Vote> fromEmojiOptional(ReactionEmoji e) {
		for (Vote v : VALUES) {
			if (v.reaction.equals(e)) {
				return Optional.of(v);
			}
		}

		return Optional.empty();
	}

	public static Vote fromEmoji(ReactionEmoji e) {
		return fromEmojiOptional(e).orElse(NONE);
	}
}
