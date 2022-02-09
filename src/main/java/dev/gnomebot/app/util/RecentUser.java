package dev.gnomebot.app.util;

import discord4j.common.util.Snowflake;

import java.util.Objects;

public record RecentUser(Snowflake id, String tag) {
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RecentUser that = (RecentUser) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
