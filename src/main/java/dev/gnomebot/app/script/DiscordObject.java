package dev.gnomebot.app.script;

import org.jetbrains.annotations.Nullable;

public class DiscordObject implements WithId {
	public final WrappedId id;

	public DiscordObject(WrappedId id) {
		this.id = id;
	}

	@Override
	public final WrappedId getWrappedId() {
		return id;
	}

	public void delete(@Nullable String reason) {
		throw new IllegalStateException("This object does not support deletion!");
	}

	public final void delete() {
		delete(null);
	}

	@Override
	public final int hashCode() {
		return id.hashCode();
	}

	@Override
	public final boolean equals(Object obj) {
		return obj == this || obj instanceof WithId w && id.asLong() == w.getWrappedId().asLong();
	}

	public String toString() {
		return id.toString();
	}
}
