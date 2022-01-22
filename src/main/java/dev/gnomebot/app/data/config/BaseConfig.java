package dev.gnomebot.app.data.config;

import com.google.gson.JsonElement;
import com.mongodb.client.model.Updates;
import dev.gnomebot.app.data.GuildCollections;

public abstract class BaseConfig<T> {
	public final GuildCollections gc;
	public final String id;
	public final T defaultValue;
	private T value;
	public String title;
	public boolean internal;

	public BaseConfig(GuildCollections g, String n, T def) {
		gc = g;
		id = n;
		defaultValue = def;
		value = defaultValue;
		title = id.replace("_", " ");
	}

	@SuppressWarnings("unchecked")
	public <C> C internal() {
		internal = true;
		return (C) this;
	}

	@SuppressWarnings("unchecked")
	public <C> C title(String t) {
		title = t;
		return (C) this;
	}

	public void set(T t) {
		value = t;
	}

	public T get() {
		return value;
	}

	public void save() {
		gc.db.guildData.query(gc.guildId.asLong()).update(Updates.set(id, toDB()));
	}

	public Object toDB() {
		return get();
	}

	@SuppressWarnings("unchecked")
	public void fromDB(Object o) {
		set((T) o);
	}

	public abstract String getType();

	public abstract JsonElement toJson();

	public abstract void fromJson(JsonElement json);

	@Override
	public String toString() {
		return String.valueOf(get());
	}
}
