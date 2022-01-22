package dev.gnomebot.app.data.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
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
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public void fromJson(JsonElement json) {
		set(json.getAsString());
	}

	public boolean isEmpty() {
		return get().isEmpty();
	}
}
