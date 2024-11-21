package dev.gnomebot.app.util;

import dev.latvian.apps.json.JSON;
import dev.latvian.apps.json.JSONArray;
import dev.latvian.apps.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ConfigFile {
	public final Path path;
	private final JSONObject options;
	private boolean needsSaving;

	public ConfigFile(Path p) {
		path = p;
		needsSaving = false;
		var o = JSONObject.of();

		if (Files.exists(path)) {
			try {
				o = JSON.DEFAULT.read(Files.readString(path)).readObject();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			needsSaving = true;
		}

		options = o;
	}

	public Object get(String key, Object def) {
		var o = options.get(key);

		if (o == null) {
			options.put(key, def);
			needsSaving = true;
			return def;
		}

		return o;
	}

	public String getString(String key, String def) {
		return String.valueOf(get(key, def));
	}

	public long getSnowflake(String key) {
		return SnowFlake.num(getString(key, ""));
	}

	public int getInt(String key, int def) {
		return ((Number) get(key, def)).intValue();
	}

	public long getLong(String key, long def) {
		return ((Number) get(key, def)).longValue();
	}

	public double getDouble(String key, double def) {
		return ((Number) get(key, def)).doubleValue();
	}

	public boolean getBoolean(String key, boolean def) {
		return (Boolean) get(key, def);
	}

	public List<String> getStringList(String key) {
		var e = get(key, JSONArray.of());

		if (e instanceof List<?> list) {
			List<String> s = new ArrayList<>();

			for (var e1 : list) {
				s.add(String.valueOf(e1));
			}

			return s;
		}

		return Collections.emptyList();
	}

	public boolean has(String key) {
		return options.containsKey(key);
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
				Files.writeString(path, JSON.DEFAULT.writePretty(options));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}
