package dev.gnomebot.app.util;

import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.latvian.apps.webutils.ansi.Ansi;
import dev.latvian.apps.webutils.data.Substitute;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class CharMap {
	public static final CharMap EMPTY = new CharMap('\0');
	public static final HashMap<Character, CharMap> MAP = new HashMap<>();
	public static final List<Substitute> SUBSTITUTES = new ArrayList<>();

	public static final Pattern MODIFIER_PATTERN = Pattern.compile("[\\u02B0-\\u02FF]");

	public static CharMap get(char c) {
		return MAP.getOrDefault(c, CharMap.EMPTY);
	}

	private static boolean in(char c, char a, char b) {
		return c >= a && c <= b;
	}

	public static boolean isWordChar(char c) {
		return in(c, 'a', 'z') || in(c, 'A', 'Z') || in(c, '0', '9');
	}

	public static boolean isNormalChar(char c) {
		return c == ' ' || isWordChar(c) || in(c, '!', '@') || in(c, '[', '`') || in(c, '{', '~') || c == '™';
	}

	public static boolean isPingable(String string) {
		if (string.length() < 2) {
			return false;
		}

		int p = 0;

		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);

			if (c == ' ') {
				p = 0;
			} else if (isWordChar(c)) {
				p++;

				if (p >= 2) {
					return true;
				}
			}
		}

		return false;
	}

	public static void load() {
		MAP.clear();
		SUBSTITUTES.clear();

		List<Character> chars = new ArrayList<>();

		for (char c = 'a'; c <= 'z'; c++) {
			chars.add(c);
		}

		for (char c = '0'; c <= '9'; c++) {
			chars.add(c);
		}

		for (Character c : chars) {
			try (BufferedReader reader = Files.newBufferedReader(AppPaths.RESOURCES.resolve("characters/" + c + ".txt"))) {
				CharMap m = MAP.computeIfAbsent(c, CharMap::new);
				String line;

				while ((line = reader.readLine()) != null) {
					if (!line.isEmpty()) {
						m.add(line);
						m.add(line.toLowerCase());
						m.add(line.toUpperCase());
					}
				}
			} catch (Exception ex) {
				App.error(Ansi.of("Failed to read special char map for ").append(Ansi.yellow(c)));
			}
		}

		for (Map.Entry<Character, CharMap> entry : new HashMap<>(MAP).entrySet()) {
			int order = 0;

			for (String c : entry.getValue().possible) {
				if (c.length() > 1 || !isWordChar(c.charAt(0))) {
					SUBSTITUTES.add(new Substitute(entry.getValue().string, c, order));
					order++;
				}
			}
		}

		Map<String, String> special = new HashMap<>();
		special.put("delete", "");
		special.put("ae", "ae");

		for (Map.Entry<String, String> entry : special.entrySet()) {
			try (BufferedReader reader = Files.newBufferedReader(AppPaths.RESOURCES.resolve("characters/" + entry.getKey() + ".txt"))) {
				String line;

				while ((line = reader.readLine()) != null) {
					if (!line.isEmpty()) {
						SUBSTITUTES.add(new Substitute(entry.getValue(), line, -1));

						String low = line.toLowerCase();
						String up = line.toUpperCase();

						if (!low.equals(line)) {
							SUBSTITUTES.add(new Substitute(entry.getValue(), low, -1));
						}

						if (!up.equals(line)) {
							SUBSTITUTES.add(new Substitute(entry.getValue(), up, -1));
						}
					}
				}
			} catch (Exception ex) {
				App.error("Failed to read " + entry.getKey() + ".txt");
			}
		}

		SUBSTITUTES.sort(null);

		//App.log("Substitutes: " + SUBSTITUTES.stream().map(Substitute::toString).collect(Collectors.joining(", ")));
	}

	private static String removeMidSpaces(String s) {
		if (s.length() % 2 == 1 && s.length() >= 3 && s.charAt(1) <= ' ') {
			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < s.length(); i++) {
				if (i % 2 == 1) {
					if (s.charAt(i) > ' ') {
						return s;
					}
				} else {
					sb.append(s.charAt(i));
				}
			}

			return sb.toString();
		}

		return s;
	}

	public static String makePingable(String string, String backup) {
		string = string.trim();

		if (isPingable(string)) {
			return string;
		}

		string = MODIFIER_PATTERN.matcher(string.trim()).replaceAll("");

		if (isPingable(string)) {
			return string;
		}

		for (var substitute : SUBSTITUTES) {
			string = string.replace(substitute.string(), substitute.with());
		}

		string = string.trim();

		if (isPingable(string)) {
			return string;
		}

		string = removeMidSpaces(string).trim();

		if (isPingable(string)) {
			return string;
		}

		string = string.replace(" ", "");

		if (isPingable(string)) {
			return string;
		}

		return backup;
	}

	public final char character;
	public final String string;

	public final Set<String> possible;
	public final Set<Character> possibleCharacters;
	public final Set<String> possibleStrings;
	public char firstPossibleCharacter;

	public CharMap(char c) {
		character = c;
		string = String.valueOf(character);
		possible = new LinkedHashSet<>();
		possibleCharacters = new LinkedHashSet<>();
		possibleStrings = new LinkedHashSet<>();
		firstPossibleCharacter = '\0';
	}

	public boolean isEmpty() {
		return firstPossibleCharacter == '\0';
	}

	public boolean contains(String s) {
		return possible.contains(s);
	}

	public boolean contains(char c) {
		return possibleCharacters.contains(c);
	}

	public void add(String s) {
		if (possible.add(s)) {
			if (s.length() == 1) {
				possibleCharacters.add(s.charAt(0));

				if (isEmpty()) {
					firstPossibleCharacter = s.charAt(0);
				}
			} else {
				possibleStrings.add(s);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		buildPattern(sb);
		return sb.toString();
	}

	public void buildPattern(StringBuilder sb) {
		if (!possibleStrings.isEmpty()) {
			sb.append("(?:");
		}

		sb.append('[');
		sb.append(character);

		for (Character c : possibleCharacters) {
			if (c != character) {
				sb.append(c);
			}
		}

		sb.append(']');

		if (!possibleStrings.isEmpty()) {
			for (String s : possibleStrings) {
				sb.append('|');
				sb.append(s);
			}

			sb.append(")");
		}
	}
}