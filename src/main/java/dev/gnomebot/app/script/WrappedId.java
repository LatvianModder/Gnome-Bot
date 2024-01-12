package dev.gnomebot.app.script;

import dev.gnomebot.app.util.SnowFlake;
import discord4j.common.util.Snowflake;
import discord4j.discordjson.Id;

public final class WrappedId implements WithId {
	public static final WrappedId NONE = new WrappedId(0L);

	private final long asLong;
	private final String asString;

	public WrappedId(Snowflake id) {
		asLong = id.asLong();
		asString = id.asString();
	}

	public WrappedId(long id) {
		asLong = id;
		asString = SnowFlake.str(id);
	}

	public WrappedId(String id) {
		asLong = SnowFlake.num(id);
		asString = id;
	}

	public WrappedId(Id id) {
		asLong = id.asLong();
		asString = id.asString();
	}

	public long asLong() {
		return asLong;
	}

	public String asString() {
		return asString;
	}

	@Override
	public WrappedId getWrappedId() {
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (o instanceof WrappedId id) {
			return asLong == id.asLong;
		} else if (o instanceof Number n) {
			return asLong == n.longValue();
		} else if (o instanceof CharSequence) {
			return asString.equals(o.toString());
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Long.hashCode(asLong);
	}

	@Override
	public String toString() {
		return asString;
	}
}
