package dev.gnomebot.app.util;

public record RecentUser(long id, String tag) {
	@Override
	public boolean equals(Object o) {
		return o == this || o instanceof RecentUser r && id == r.id;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(id);
	}
}
