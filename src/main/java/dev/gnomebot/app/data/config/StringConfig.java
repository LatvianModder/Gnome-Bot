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

	public boolean isEmpty() {
		return get().isEmpty();
	}
}
