package dev.gnomebot.app.data.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
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
	public void set(Integer i) {
		super.set(Math.min(Math.max(minValue, i), maxValue));
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public void fromJson(JsonElement json) {
		set(json.getAsInt());
	}
}
