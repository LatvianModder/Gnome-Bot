package dev.gnomebot.app.util;

import org.jetbrains.annotations.Nullable;

public class MutableLong {
	public long value = 0L;

	public MutableLong() {
	}

	public MutableLong(long v) {
		value = v;
	}

	public MutableLong(Object key) {
	}

	public void add(long v) {
		value += v;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	public static long valueOf(@Nullable MutableLong v) {
		return v == null ? 0L : v.value;
	}
}
