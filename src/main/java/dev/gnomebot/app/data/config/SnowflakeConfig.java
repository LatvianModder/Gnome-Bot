package dev.gnomebot.app.data.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;

public abstract class SnowflakeConfig extends BaseConfig<Snowflake> {
	public SnowflakeConfig(GuildCollections g, String n) {
		super(g, n, Utils.NO_SNOWFLAKE);
	}

	@Override
	public void set(Snowflake s) {
		super.set(s.asLong() == 0L ? Utils.NO_SNOWFLAKE : s);
	}

	@Override
	public Object toDB() {
		return get().asLong();
	}

	@Override
	public void fromDB(Object o) {
		set(Snowflake.of(((Number) o).longValue()));
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get().asString());
	}

	@Override
	public void fromJson(JsonElement json) {
		set(Snowflake.of(json.getAsString()));
	}

	public boolean isSet() {
		return get().asLong() != 0L;
	}
}
