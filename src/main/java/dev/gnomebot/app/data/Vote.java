package dev.gnomebot.app.data;

import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.util.Utils;
import discord4j.core.object.emoji.Emoji;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public enum Vote {
	NONE(null, Emojis.VOTENONE),
	UP(Boolean.TRUE, Emojis.VOTEUP),
	DOWN(Boolean.FALSE, Emojis.VOTEDOWN);

	public static final Vote[] VALUES = values();

	public final Boolean value;
	public final Emoji reaction;

	Vote(@Nullable Boolean v, Emoji r) {
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

	public static Optional<Vote> fromEmojiOptional(Emoji e) {
		for (var v : VALUES) {
			if (v.reaction.equals(e)) {
				return Optional.of(v);
			}
		}

		return Optional.empty();
	}

	public static Vote fromEmoji(Emoji e) {
		return fromEmojiOptional(e).orElse(NONE);
	}
}
