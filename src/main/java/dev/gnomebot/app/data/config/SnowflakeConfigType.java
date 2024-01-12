package dev.gnomebot.app.data.config;

import dev.gnomebot.app.util.SnowFlake;

public interface SnowflakeConfigType<H extends ConfigHolder<Long>> extends ConfigType<Long, H> {
	@Override
	default Long defaultKeyValue() {
		return 0L;
	}

	@Override
	default Object write(Long value) {
		return SnowFlake.str(value);
	}

	@Override
	default Long read(Object value) {
		return SnowFlake.num(String.valueOf(value));
	}
}
