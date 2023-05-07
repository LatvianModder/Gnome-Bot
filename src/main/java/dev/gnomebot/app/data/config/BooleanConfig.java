package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;

public class BooleanConfig extends BaseConfig<Boolean> {
	public BooleanConfig(GuildCollections g, String n, boolean def) {
		super(g, n, def);
	}

	@Override
	public String getType() {
		return "boolean";
	}
}
