package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.util.Utils;

import java.util.regex.Pattern;

public record Ping(Pattern pattern, boolean allow) {
	public static final Ping[] NO_PINGS = new Ping[0];

	@Override
	public String toString() {
		return (allow ? "+" : "-") + Utils.toRegexString(pattern);
	}
}
