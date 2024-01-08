package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class ConfigHolder<T> implements Supplier<T> {
	public final GuildCollections gc;
	public final ConfigKey<T, ?> key;
	public T value;
	public boolean save;

	public ConfigHolder(GuildCollections gc, ConfigKey<T, ?> key) {
		this.gc = gc;
		this.key = key;
		this.value = key.defaultValue();
		this.save = false;
	}

	@Override
	public T get() {
		return value;
	}

	public void set(@Nullable T val) {
		value = val;
		save = true;
		gc.saveConfig();
	}

	public String validate(int type, String value) {
		return key.type().validate(gc, type, value);
	}

	public String serialize(int type) {
		return key.type().serialize(gc, type, get());
	}

	public void deserialize(int type, String val) {
		value = key.type().deserialize(gc, type, val);
		save = true;
		gc.saveConfig();
	}

	public Object write() {
		return key.type().write(get());
	}

	public void read(Object val) {
		value = key.type().read(val);
		save = true;
	}
}
