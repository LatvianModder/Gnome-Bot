package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;

public class IntConfig extends BaseConfig<Integer> {
	public final int minValue;
	public final int maxValue;

	public IntConfig(GuildCollections g, String n, int def, int min, int max) {
		super(g, n, def);
		minValue = min;
		maxValue = max;
	}

	public IntConfig(GuildCollections g, String n, int def) {
		this(g, n, def, 0, Integer.MAX_VALUE);
	}

	@Override
	public String getType() {
		return "int";
	}

	@Override
	public String serialize() {
		return get().toString();
	}

	@Override
	public void deserialize(String value) {
		set(Integer.parseInt(value));
	}

	@Override
	public void set(Integer i) {
		super.set(Math.min(Math.max(minValue, i), maxValue));
	}
}
