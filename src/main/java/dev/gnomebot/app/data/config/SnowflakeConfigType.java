package dev.gnomebot.app.data.config;

import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;

public interface SnowflakeConfigType<H extends ConfigHolder<Snowflake>> extends ConfigType<Snowflake, H> {
	@Override
	default Snowflake defaultKeyValue() {
		return Utils.NO_SNOWFLAKE;
	}

	@Override
	default Object write(Snowflake value) {
		return value.asString();
	}

	@Override
	default Snowflake read(Object value) {
		return Utils.snowflake(String.valueOf(value));
	}
}
