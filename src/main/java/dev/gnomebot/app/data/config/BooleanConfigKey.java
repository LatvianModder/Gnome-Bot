package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;

public class BooleanConfigKey extends ConfigKey<Boolean> {
	public BooleanConfigKey(GuildCollections g, String n, boolean def) {
		super(g, n, def);
	}

	@Override
	public String getType() {
		return "boolean";
	}

	@Override
	public String serialize() {
		return get().toString();
	}

	@Override
	public void deserialize(String value) {
		set(Boolean.parseBoolean(value));
	}
}
