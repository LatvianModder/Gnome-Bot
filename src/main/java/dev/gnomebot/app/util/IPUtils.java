package dev.gnomebot.app.util;

public class IPUtils {
	public static long asLong(int a, int b, int c, int d) {
		return (((long) a) << 24) | (((long) b) << 16) | (((long) c) << 8) | ((long) d);
	}

	public record IPRange(long start, long end) {
		boolean contains(long ip) {
			return ip >= start && ip <= end;
		}
	}

	public static long ipToLong(String ip) {
		String[] s = ip.split("\\.", 4);
		return asLong(Integer.parseInt(s[0]), Integer.parseInt(s[1]), Integer.parseInt(s[2]), Integer.parseInt(s[3]));
	}

	public static IPRange range(String s) {
		String[] s1 = s.split("-", 2);
		long start = ipToLong(s1[0]);
		return s1.length == 2 ? new IPRange(start, ipToLong(s1[1])) : new IPRange(start, start);
	}

	// https://en.wikipedia.org/wiki/Reserved_IP_addresses

	public static final IPRange[] RESERVED = new IPRange[]{
			range("0.0.0.0-0.255.255.255"),
			range("10.0.0.0-10.255.255.255"),
			range("100.64.0.0-100.127.255.255"),
			range("127.0.0.0-127.255.255.255"),
			range("169.254.0.0-169.254.255.255"),
			range("172.16.0.0-172.31.255.255"),
			range("192.0.0.0-192.0.0.255"),
			range("192.0.2.0-192.0.2.255"),
			range("192.88.99.0-192.88.99.255"),
			range("192.168.0.0-192.168.255.255"),
			range("198.18.0.0-198.19.255.255"),
			range("198.51.100.0-198.51.100.255"),
			range("203.0.113.0-203.0.113.255"),
			range("224.0.0.0-239.255.255.255"),
			range("233.252.0.0-233.252.0.255"),
			range("240.0.0.0-255.255.255.254"),
			range("255.255.255.255"),
	};

	public static boolean isReserved(long ip) {
		for (IPRange r : RESERVED) {
			if (r.contains(ip)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isIP(int a, int b, int c, int d) {
		if (a == 0 || a == 255) {
			return false;
		} else if (a < 0 || a > 255 || b < 0 || b > 255 || c < 0 || c > 255 || d < 0 || d > 255) {
			return false;
		}

		return !isReserved(asLong(a, b, c, d));
	}
}
