package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import org.bson.Document;

public abstract class SnowflakeConfigKey extends ConfigKey<Snowflake> {
	public SnowflakeConfigKey(GuildCollections g, String n) {
		super(g, n, Utils.NO_SNOWFLAKE);
	}

	@Override
	public String serialize() {
		return get().asString();
	}

	@Override
	public void deserialize(String value) {
		set(Snowflake.of(value));
	}

	@Override
	public void set(Snowflake s) {
		super.set(s.asLong() == 0L ? Utils.NO_SNOWFLAKE : s);
	}

	@Override
	public Object write() {
		return get().asLong();
	}

	@Override
	public void read(Document document) {
		set(Snowflake.of(document.getLong(id)));
	}
}
