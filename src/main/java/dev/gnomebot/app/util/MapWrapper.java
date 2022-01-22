package dev.gnomebot.app.util;

import com.google.gson.JsonObject;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class MapWrapper {
	public static final MapWrapper EMPTY = new MapWrapper(Collections.emptyMap());

	public final Map<String, Object> map;

	private MapWrapper(Map<String, Object> m) {
		map = m;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static MapWrapper wrap(@Nullable Object o) {
		if (o instanceof MapWrapper) {
			return (MapWrapper) o;
		} else if (o instanceof Map) {
			return new MapWrapper((Map) o);
		}

		return EMPTY;
	}

	@Nullable
	public Object get(String key, @Nullable Object def) {
		return map.isEmpty() ? def : map.getOrDefault(key, def);
	}

	public MapWrapper getMap(String key) {
		return wrap(get(key, null));
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public <E> List<E> getList(String key) {
		Object o = get(key, null);

		if (o instanceof List) {
			return (List) o;
		} else if (o instanceof Iterable) {
			List<E> list = new ArrayList<>();

			for (E o1 : (Iterable<E>) o) {
				list.add(o1);
			}

			return list;
		}

		return Collections.emptyList();
	}

	@Nullable
	public ObjectId getObjectId(String key) {
		return (ObjectId) get(key, null);
	}

	public String getString(String key, String def) {
		return String.valueOf(get(key, def));
	}

	public String getString(String key) {
		return getString(key, "");
	}

	@Nullable
	public Boolean getNullableBoolean(String key) {
		Object o = get(key, null);
		return o instanceof Boolean ? ((Boolean) o) : null;
	}

	public boolean getBoolean(String key, boolean def) {
		Boolean o = getNullableBoolean(key);
		return o == null ? def : o;
	}

	public boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	public Number getNumber(String key, Number def) {
		Object o = get(key, def);
		return o instanceof Number ? ((Number) o) : def;
	}

	public long getLong(String key, long def) {
		return getNumber(key, def).longValue();
	}

	public long getLong(String key) {
		return getLong(key, 0L);
	}

	public int getInt(String key, int def) {
		return getNumber(key, def).intValue();
	}

	public int getInt(String key) {
		return getInt(key, 0);
	}

	public double getDouble(String key, double def) {
		return getNumber(key, def).doubleValue();
	}

	public double getDouble(String key) {
		return getDouble(key, 0D);
	}

	@Nullable
	public Date getDate(String key) {
		return (Date) get(key, null);
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();

		for (Map.Entry<String, Object> entry : map.entrySet()) {
			json.add(entry.getKey(), Utils.toJson(entry.getValue()));
		}

		return json;
	}
}