package dev.gnomebot.app.util;

import dev.gnomebot.app.AppPaths;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class EmojiMap {
	public static final Pattern UNICODE_EMOJI_PATTERN = Pattern.compile("\\u00a9|\\u00ae|[\\u2000-\\u3300]|[\\ud83c-\\ud83e][\\ud000-\\udfff]", Pattern.UNICODE_CHARACTER_CLASS);
	public static final Pattern RAW_UNICODE_EMOJI_PATTERN = Pattern.compile("\u00a9|\u00ae|[\u2000-\u3300]|[\ud83c-\ud83e][\ud000-\udfff]", Pattern.UNICODE_CHARACTER_CLASS);

	private static Map<String, List<String>> unicodeToNames;
	private static Map<String, String> nameToUnicode;
	private static Pattern unicodePattern;

	public static Map<String, List<String>> getUnicodeToNames() {
		if (unicodeToNames == null) {
			unicodeToNames = new LinkedHashMap<>();
		}

		return unicodeToNames;
	}

	public static Map<String, String> getNameToUnicode() {
		if (nameToUnicode == null) {
			nameToUnicode = new LinkedHashMap<>();

			try (BufferedReader reader = Files.newBufferedReader(AppPaths.RESOURCES.resolve("emoji_list.json"))) {
				String line;

				while ((line = reader.readLine()) != null) {
					String[] s = line.trim().split(" ", 2);

					if (s.length == 2 && !s[0].isEmpty() && !s[1].isEmpty()) {
						nameToUnicode.put(s[0], s[1]);
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return nameToUnicode;
	}

	public static Pattern getUnicodePattern() {
		if (unicodePattern == null) {
			unicodePattern = Pattern.compile(String.join("|", getUnicodeToNames().keySet()));
		}

		return unicodePattern;
	}
}
