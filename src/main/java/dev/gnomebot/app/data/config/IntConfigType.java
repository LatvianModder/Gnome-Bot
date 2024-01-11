package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;

public record IntConfigType(int min, int max) implements BasicConfigType<Integer> {
	public static final IntConfigType DEFAULT = new IntConfigType(Integer.MIN_VALUE, Integer.MAX_VALUE);
	public static final IntConfigType POSITIVE = new IntConfigType(0, Integer.MAX_VALUE);

	@Override
	public String getTypeName() {
		return "int";
	}

	@Override
	public Integer defaultKeyValue() {
		return 0;
	}

	@Override
	public Integer read(Object value) {
		return ((Number) value).intValue();
	}

	@Override
	public String validate(GuildCollections guild, int type, String value) {
		try {
			var val = Integer.parseInt(value);

			if (val < min || val > max) {
				if (max == Integer.MAX_VALUE) {
					return "Value must be larger than " + min + "!";
				} else {
					return "Value must be between " + min + " and " + max + "!";
				}
			}
		} catch (Exception ignored) {
			return "Value must be an integer!";
		}

		return "";
	}

	@Override
	public String serialize(GuildCollections guild, int type, Integer value) {
		return value.toString();
	}

	@Override
	public Integer deserialize(GuildCollections guild, int type, String value) {
		return Integer.parseInt(value);
	}
}
