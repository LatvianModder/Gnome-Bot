package dev.gnomebot.app.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import discord4j.common.util.Snowflake;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ConfigFile {
	public final Path path;
	private final JsonObject options;
	private boolean needsSaving;

	public ConfigFile(Path p) {
		path = p;
		needsSaving = false;
		JsonObject o = new JsonObject();

		if (Files.exists(path)) {
			try {
				o = Utils.GSON_PRETTY.fromJson(Files.readString(path), JsonObject.class);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			needsSaving = true;
		}

		options = o;
	}

	public JsonElement get(String key, JsonElement def) {
		JsonElement o = options.get(key);

		if (o == null || o.isJsonNull()) {
			options.add(key, def);
			needsSaving = true;
			return def;
		}

		return o;
	}

	public String getString(String key, String def) {
		return get(key, new JsonPrimitive(def)).getAsString();
	}

	public Snowflake getSnowflake(String key) {
		String s = getString(key, "");
		return s.isEmpty() || s.equals("0") ? Utils.NO_SNOWFLAKE : Snowflake.of(s);
	}

	public int getInt(String key, int def) {
		return get(key, new JsonPrimitive(def)).getAsInt();
	}

	public long getLong(String key, long def) {
		return get(key, new JsonPrimitive(def)).getAsLong();
	}

	public double getDouble(String key, double def) {
		return get(key, new JsonPrimitive(def)).getAsDouble();
	}

	public boolean getBoolean(String key, boolean def) {
		return get(key, new JsonPrimitive(def)).getAsBoolean();
	}

	public List<String> getStringList(String key) {
		JsonElement e = get(key, new JsonArray());

		if (e.isJsonArray()) {
			List<String> s = new ArrayList<>();

			for (JsonElement e1 : e.getAsJsonArray()) {
				s.add(e1.getAsString());
			}

			return s;
		}

		return Collections.emptyList();
	}

	public boolean has(String key) {
		return options.has(key);
	}

	public void remove(String key) {
		if (options.remove(key) != null) {
			needsSaving = true;
		}
	}

	public Set<String> getKeys() {
		return options.keySet();
	}

	public void needsSaving() {
		needsSaving = true;
	}

	public void save() {
		if (needsSaving) {
			try {
				Files.writeString(path, Utils.GSON_PRETTY.toJson(options));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
