package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;

import java.util.Collection;
import java.util.Set;

public interface ConfigType<T, H extends ConfigHolder<T>> {
	String getTypeName();

	H createHolder(GuildCollections gc, ConfigKey<T, H> key);

	T defaultKeyValue();

	default Object write(T value) {
		return value;
	}

	T read(Object value);

	default String validate(GuildCollections guild, int type, String value) {
		return "";
	}

	String serialize(GuildCollections guild, int type, T value);

	T deserialize(GuildCollections guild, int type, String value);

	default boolean hasEnumValues() {
		return false;
	}

	default Collection<EnumValue> getEnumValues(GuildCollections guild) {
		return Set.of();
	}

	default ListConfigType<T> asList() {
		return new ListConfigType<>(this);
	}
}
