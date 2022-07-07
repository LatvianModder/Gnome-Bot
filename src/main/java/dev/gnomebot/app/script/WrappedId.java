package dev.gnomebot.app.script;

import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.discordjson.Id;

public final class WrappedId implements WithId {
	public static final WrappedId NONE = new WrappedId(Utils.NO_SNOWFLAKE);

	private final String asString;
	private final long asLong;

	public WrappedId(Snowflake id) {
		asString = id.asString();
		asLong = id.asLong();
	}

	public WrappedId(Id id) {
		asString = id.asString();
		asLong = id.asLong();
	}

	public long asLong() {
		return asLong;
	}

	public String asString() {
		return asString;
	}

	public Snowflake asSnowflake() {
		return Snowflake.of(asLong);
	}

	public Id asId() {
		return Id.of(asLong);
	}

	@Override
	public WrappedId getWrappedId() {
		return this;
	}

	@Override
	public boolean equals(Object o) {
		return o == this || o instanceof WrappedId && asLong == ((WrappedId) o).asLong;
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
