package dev.gnomebot.app.data.config;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FontConfigType implements ConfigType<Font, FontConfigType.Holder> {
	public static final FontConfigType DEFAULT = new FontConfigType();
	private static final Pattern STLYE_PATTERN = Pattern.compile("^([!/]+\\s+)?(.*)$");

	public static final Map<String, Font> ALL_FONTS = Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts())
			.sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
			.collect(Collectors.toMap(Font::getName, Function.identity(), (font, font2) -> font, LinkedHashMap::new));

	private static Font defaultFont = ALL_FONTS.get("DejaVu Serif");

	static {
		App.info("Available Fonts: " + String.join(" ; ", ALL_FONTS.keySet()));

		if (defaultFont == null) {
			defaultFont = ALL_FONTS.values().iterator().next();
		}
	}

	public static class Holder extends ConfigHolder<Font> {
		public Holder(GuildCollections gc, ConfigKey<Font, Holder> key) {
			super(gc, key);
		}

		public Font create(float size) {
			return get().deriveFont(size);
		}
	}

	@Override
	public String getTypeName() {
		return "font";
	}

	@Override
	public Holder createHolder(GuildCollections gc, ConfigKey<Font, Holder> key) {
		return new Holder(gc, key);
	}

	@Override
	public Font defaultKeyValue() {
		return defaultFont;
	}

	@Override
	public String write(Font value) {
		var sb = new StringBuilder();

		if (value.isBold()) {
			sb.append('!');
		}

		if (value.isItalic()) {
			sb.append('/');
		}

		if (value.isBold() || value.isItalic()) {
			sb.append(' ');
		}

		sb.append(value.getName());
		return sb.toString();
	}

	@Override
	public Font read(Object value) {
		var match = STLYE_PATTERN.matcher(String.valueOf(value));

		if (match.find()) {
			var font = ALL_FONTS.get(match.group(2));

			if (font != null) {
				var style = 0;
				var styleChars = match.group(1);

				if (styleChars != null && !styleChars.isEmpty()) {
					for (var c : styleChars.toCharArray()) {
						switch (c) {
							case '!' -> style |= Font.BOLD;
							case '/' -> style |= Font.ITALIC;
						}
					}
				}

				return style == 0 ? font : font.deriveFont(style);
			}
		}

		return null;
	}

	@Override
	public String validate(GuildCollections guild, int type, String value) {
		var f = read(value);
		return f != null ? "" : "Font not found!";
	}

	@Override
	public String serialize(GuildCollections guild, int type, Font value) {
		return write(value);
	}

	@Override
	public Font deserialize(GuildCollections guild, int type, String value) {
		return read(value);
	}

	@Override
	public boolean hasEnumValues() {
		return true;
	}

	@Override
	public Collection<EnumValue> getEnumValues(GuildCollections guild) {
		return ALL_FONTS.keySet().stream().map(EnumValue::new).toList();
	}
}
