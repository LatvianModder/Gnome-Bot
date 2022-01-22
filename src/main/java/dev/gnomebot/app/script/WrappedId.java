package dev.gnomebot.app.script;

import discord4j.common.util.Snowflake;
import discord4j.discordjson.Id;

public final class WrappedId implements WithId {
	public final String asString;
	public final long asLong;

	public WrappedId(Snowflake id) {
		asString = id.asString();
		asLong = id.asLong();
	}

	public WrappedId(Id id) {
		asString = id.asString();
		asLong = id.asLong();
	}

	@Override
	public WrappedId id() {
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
