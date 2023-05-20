package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;

public class StringConfig extends BaseConfig<String> {
	public StringConfig(GuildCollections g, String n, String def) {
		super(g, n, def);
	}

	@Override
	public String getType() {
		return "string";
	}

	@Override
	public String serialize() {
		return get();
	}

	@Override
	public void deserialize(String value) {
		set(value);
	}

	public boolean isEmpty() {
		return get().isEmpty();
	}
}
