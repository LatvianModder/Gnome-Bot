package dev.gnomebot.app.script;

import dev.latvian.mods.rhino.util.SpecialEquality;
import discord4j.common.util.Snowflake;

import java.time.Instant;
import java.util.Date;

public interface WithId extends SpecialEquality {
	WrappedId id();

	default long getCreatedTimestampMilli() {
		return Snowflake.DISCORD_EPOCH + (id().asLong >>> 22);
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
		} else if (o instanceof WrappedId) {
			return id().asLong == ((WrappedId) o).asLong;
		} else if (o instanceof Snowflake) {
			return id().asLong == ((Snowflake) o).asLong();
		} else if (o instanceof String) {
			return id().asString.equals(o);
		} else if (o instanceof Number) {
			return id().asLong == ((Number) o).longValue();
		}

		return false;
	}
}
