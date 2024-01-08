package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;

public interface BasicConfigType<T> extends ConfigType<T, ConfigHolder<T>> {
	@Override
	default ConfigHolder<T> createHolder(GuildCollections gc, ConfigKey<T, ConfigHolder<T>> key) {
		return new ConfigHolder<>(gc, key);
	}
}
