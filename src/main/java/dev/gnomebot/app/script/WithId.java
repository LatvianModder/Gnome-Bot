package dev.gnomebot.app.script;

import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.util.SpecialEquality;
import discord4j.common.util.Snowflake;

import java.time.Instant;
import java.util.Date;

public interface WithId extends SpecialEquality {
	WrappedId getWrappedId();

	default long getCreatedTimestampMilli() {
		return SnowFlake.timestamp(getWrappedId().asLong());
	}

	default Date getCreatedTimestamp() {
		return new Date(getCreatedTimestampMilli());
	}

	default String getCreatedTimestampString() {
		return Instant.ofEpochMilli(getCreatedTimestampMilli()).toString();
	}

	@Override
	default boolean specialEquals(Context cx, Object o, boolean shallow) {
		if (o == this) {
			return true;
		} else if (o instanceof WrappedId i) {
			return getWrappedId().asLong() == i.asLong();
		} else if (o instanceof Snowflake s) {
			return getWrappedId().asLong() == s.asLong();
		} else if (o instanceof String) {
			return getWrappedId().asString().equals(o);
		} else if (o instanceof Number n) {
			return getWrappedId().asLong() == n.longValue();
		}

		return false;
	}
}
