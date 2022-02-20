package dev.gnomebot.app.util;

import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleStringReader {
	public static final Pattern SIMPLE_TIME_PATTERN = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);

	protected final String string;
	public int position;

	public SimpleStringReader(String s) {
		string = s;
	}

	public Optional<String> readRemainingString() {
		String s = position >= string.length() ? "" : string.substring(position).trim();
		return s.isEmpty() ? Optional.empty() : Optional.of(s);
	}

	public List<String> readRemainingStringLines() {
		String s = readRemainingString().orElse("");
		return s.isEmpty() ? Collections.emptyList() : Arrays.asList(s.split("\n"));
	}

	public char readChar() {
		if (position >= string.length()) {
			return 0;
		}

		char c = string.charAt(position);
		position++;
		return c;
	}

	public char peekChar() {
		if (position >= string.length()) {
			return 0;
		}

		return string.charAt(position);
	}

	public Optional<String> readString() {
		char c = peekChar();

		if (c == 0) {
			return Optional.empty();
		}

		if (c <= ' ') {
			while ((c = peekChar()) <= ' ') {
				readChar();
			}
		}

		StringBuilder sb = new StringBuilder();

		if (c == '"') {
			readChar();

			char pc = 0;

			while ((c = readChar()) != 0) {
				if (c == '"' && pc != '\\') {
					break;
				} else {
					sb.append(c);
				}

				pc = c;
			}
		} else {
			while ((c = readChar()) > ' ') {
				sb.append(c);
			}
		}

		String s = sb.toString();
		return s.isEmpty() ? Optional.empty() : Optional.of(s);
	}

	public Optional<String> peekString() {
		int p = position;
		Optional<String> s = readString();
		position = p;
		return s;
	}

	public Optional<Boolean> readBoolean() {
		int p = position;
		Optional<String> s = readString();

		if (s.isEmpty()) {
			return Optional.empty();
		}

		if (s.get().equalsIgnoreCase("true")) {
			return Optional.of(Boolean.TRUE);
		} else if (s.get().equalsIgnoreCase("false")) {
			return Optional.of(Boolean.FALSE);
		}

		throw new DiscordCommandException("Failed to parse boolean!").pos(p);
	}

	public Optional<Long> readLong() throws DiscordCommandException {
		int p = position;
		Optional<String> s = readString();

		if (s.isEmpty()) {
			return Optional.empty();
		}

		try {
			return Optional.of(Long.parseLong(s.get()));
		} catch (Exception ex) {
			throw new DiscordCommandException("Failed to parse number!").pos(p);
		}
	}

	public OptionalLong readSeconds() {
		int p = position;
		Optional<String> s = readString();

		if (s.isEmpty()) {
			return OptionalLong.empty();
		}

		if (s.get().equals("-") || s.get().equalsIgnoreCase("none") || s.get().equals("0") || s.get().equalsIgnoreCase("all") || s.get().equalsIgnoreCase("forever") || s.get().equalsIgnoreCase("infinite") || s.get().equalsIgnoreCase("infinity")) {
			return OptionalLong.of(0L);
		} else if (s.get().equals("recent")) {
			return OptionalLong.of(90L * 86400L);
		} else if (s.get().equals("perm") || s.get().equals("permanent")) {
			return OptionalLong.of(100L * 365L * 86400L);
		}

		Matcher simpleMatcher = SIMPLE_TIME_PATTERN.matcher(s.get());
		long t;
		long t1;

		if (simpleMatcher.matches()) {
			try {
				t = Long.parseLong(simpleMatcher.group(1));
			} catch (Exception ex) {
				throw new DiscordCommandException("Failed to parse number!").pos(p);
			}

			t1 = switch (simpleMatcher.group(2)) {
				case "s" -> t;
				case "m" -> t * 60L;
				case "h" -> t * 3600L;
				case "d" -> t * 86400L;
				case "w" -> t * 7L * 86400L;
				case "y" -> t * 365L * 86400L;
				default -> throw new DiscordCommandException("Unknown time multiplier: " + simpleMatcher.group(2) + "!").pos(p);
			};
		} else {
			try {
				t = Long.parseLong(s.get());
			} catch (Exception ex) {
				throw new DiscordCommandException("Failed to parse number!").pos(p);
			}

			if (t <= 0L) {
				throw new DiscordCommandException("Invalid timespan!").pos(p);
			}

			int p1 = position;
			String q = readString().orElseThrow(() -> new DiscordCommandException("Missing time quantifier!").pos(p1)).toLowerCase();

			if (q.startsWith("s")) {
				t1 = t;
			} else if (q.startsWith("min")) {
				t1 = t * 60L;
			} else if (q.startsWith("h")) {
				t1 = t * 3600L;
			} else if (q.startsWith("d")) {
				t1 = t * 86400L;
			} else if (q.startsWith("w")) {
				t1 = t * 7L * 86400L;
			} else if (q.startsWith("month")) {
				t1 = t * 30L * 86400L;
			} else if (q.startsWith("y")) {
				t1 = t * 365L * 86400L;
			} else {
				throw new DiscordCommandException("Invalid time quantifier!").pos(p1);
			}
		}

		String next = peekString().orElse("");

		if (next.equalsIgnoreCase("and") || next.equals("&") || next.equals("+")) {
			readString();
			long t2 = readSeconds().orElse(0L);

			if (t2 > 0L) {
				return OptionalLong.of(t1 + t2);
			}
		}

		return OptionalLong.of(t1);
	}

	public OptionalLong readDays() {
		int p = position;
		OptionalLong s = readSeconds();

		if (s.isEmpty() || s.getAsLong() == 0L) {
			return s;
		}

		long l = s.getAsLong() / 86400L;

		if (l <= 0L) {
			throw new DiscordCommandException("Invalid timespan!").pos(p);
		}

		return OptionalLong.of(l);
	}
}
