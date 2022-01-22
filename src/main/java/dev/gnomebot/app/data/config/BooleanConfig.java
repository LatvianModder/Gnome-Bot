package dev.gnomebot.app.data.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.gnomebot.app.data.GuildCollections;

public class BooleanConfig extends BaseConfig<Boolean> {
	public BooleanConfig(GuildCollections g, String n, boolean def) {
		super(g, n, def);
	}

	@Override
	public String getType() {
		return "boolean";
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public void fromJson(JsonElement json) {
		set(json.getAsBoolean());
	}
}
