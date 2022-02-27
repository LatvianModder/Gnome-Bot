package dev.gnomebot.app.util;

import dev.gnomebot.app.discord.legacycommand.GnomeException;

public class TimeLimitedCharSequence implements CharSequence {
	private final CharSequence parent;
	private final long timeoutAfterTimestamp;

	public TimeLimitedCharSequence(CharSequence parent, long timeoutInMilliseconds) {
		this.parent = parent;
		timeoutAfterTimestamp = System.currentTimeMillis() + timeoutInMilliseconds;
	}

	public char charAt(int index) {
		if (System.currentTimeMillis() > timeoutAfterTimestamp) {
			throw new GnomeException("RegEx compile timeout!");
		}

		return parent.charAt(index);
	}

	public int length() {
		return parent.length();
	}

	public CharSequence subSequence(int start, int end) {
		return new TimeLimitedCharSequence(parent.subSequence(start, end), timeoutAfterTimestamp - System.currentTimeMillis());
	}

	@Override
	public String toString() {
		return parent.toString();
	}
}