package dev.gnomebot.app.script;

import dev.latvian.mods.rhino.util.SpecialEquality;
import discord4j.common.util.Snowflake;

import java.time.Instant;
import java.util.Date;

public interface WithId extends SpecialEquality {
	WrappedId id();

	default long getCreatedTimestampMilli() {
		return Snowflake.DISCORD_EPOCH + (id().asLong() >>> 22);
	}

	default Date getCreatedTimestamp() {
		return new Date(getCreatedTimestampMilli());
	}

	default String getCreatedTimestampString() {
		return Instant.ofEpochMilli(getCreatedTimestampMilli()).toString();
	}

	@Override
	default boolean specialEquals(Object o, boolean shallow) {
		if (o == this) {
			return true;
		} else if (o instanceof WrappedId i) {
			return id().asLong() == i.asLong();
		} else if (o instanceof Snowflake s) {
			return id().asLong() == s.asLong();
		} else if (o instanceof String) {
			return id().asString().equals(o);
		} else if (o instanceof Number n) {
			return id().asLong() == n.longValue();
		}

		return false;
	}
}
