package dev.gnomebot.app.data.config;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.data.GuildCollections;
import dev.latvian.apps.webutils.data.Possible;
import org.bson.Document;

import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public abstract class ConfigKey<T> implements Supplier<T> {
	public final GuildCollections gc;
	public final String id;
	public final T defaultValue;
	private Possible<T> value;
	public String title;
	public boolean internal;
	public Supplier<List<EnumValue>> enumValues;

	public ConfigKey(GuildCollections g, String n, T def) {
		gc = g;
		id = n;
		defaultValue = def;
		value = Possible.absent();
		title = id.replace("_", " ");
		internal = true;
	}

	public <C> C title(String t) {
		title = t;
		internal = false;
		return (C) this;
	}

	public <C> C enumValues(Supplier<List<EnumValue>> e) {
		enumValues = e;
		return (C) this;
	}

	public boolean isSet() {
		return value.isSet();
	}

	public void set(T t) {
		value = Possible.of(t);
	}

	public void unset() {
		value = Possible.absent();
	}

	@Override
	public T get() {
		return value.isAbsent() ? defaultValue : value.value();
	}

	public void save() {
		if (isSet()) {
			gc.db.guildData.query(gc.guildId.asLong()).update(Updates.set(id, write()));
		} else {
			gc.db.guildData.query(gc.guildId.asLong()).update(Updates.unset(id));
		}
	}

	public abstract String getType();

	public abstract String serialize();

	public abstract void deserialize(String value);

	public Object write() {
		return get();
	}

	public void read(Document document) {
		set(document.get(id, defaultValue));
	}

	@Override
	public String toString() {
		return String.valueOf(get());
	}
}
