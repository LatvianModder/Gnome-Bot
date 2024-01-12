package dev.gnomebot.app.util;

import discord4j.common.util.Snowflake;

public class SnowFlake {
	public static String str(long id) {
		return Long.toUnsignedString(id);
	}

	public static long num(String id) {
		if (!id.isEmpty() && !id.equals("0")) {
			try {
				return Long.parseUnsignedLong(id);
			} catch (Exception ignore) {
			}
		}

		return 0L;
	}

	public static Snowflake convert(long id) {
		return Snowflake.of(id);
	}

	public static Snowflake convert(String id) {
		return Snowflake.of(num(id));
	}

	public static long timestamp(long id) {
		return 1420070400000L + (id >>> 22);
	}

	public static long oldest(long a, long b) {
		return timestamp(a) < timestamp(b) ? a : b;
	}

	public static long newest(long a, long b) {
		return timestamp(a) > timestamp(b) ? a : b;
	}
}
