package dev.gnomebot.app.util;

public class LeaderboardEntry implements Comparable<LeaderboardEntry> {
	public long id;
	public String name;
	public long value;

	public LeaderboardEntry(long id) {
		this.id = id;
	}

	public LeaderboardEntry(String name) {
		this.name = name;
	}

	@Override
	public int compareTo(LeaderboardEntry o) {
		if (value == o.value) {
			return Long.compare(id, o.id);
		}

		return Long.compare(o.value, value);
	}
}
