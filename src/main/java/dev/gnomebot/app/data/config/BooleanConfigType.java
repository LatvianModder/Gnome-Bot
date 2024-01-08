package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;

import java.util.List;

public class BooleanConfigType implements BasicConfigType<Boolean> {
	public static final BooleanConfigType DEFAULT = new BooleanConfigType();
	private static final List<EnumValue> ENUM_VALUES = List.of(new EnumValue("true"), new EnumValue("false"));

	@Override
	public String getTypeName() {
		return "boolean";
	}

	@Override
	public Boolean defaultKeyValue() {
		return Boolean.FALSE;
	}

	@Override
	public Boolean read(Object value) {
		return (Boolean) value;
	}

	@Override
	public String validate(GuildCollections guild, int type, String value) {
		return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false") ? "" : "Value must be either true or false!";
	}

	@Override
	public String serialize(GuildCollections guild, int type, Boolean value) {
		return value.toString();
	}

	@Override
	public Boolean deserialize(GuildCollections guild, int type, String value) {
		return value.equalsIgnoreCase("true");
	}

	@Override
	public boolean hasEnumValues() {
		return true;
	}

	@Override
	public List<EnumValue> getEnumValues(GuildCollections guild) {
		return ENUM_VALUES;
	}
}
