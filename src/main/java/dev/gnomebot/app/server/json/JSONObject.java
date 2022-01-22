package dev.gnomebot.app.server.json;

import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 * @author LatvianModder
 */
public class JSONObject extends HashMap<String, Object> implements JSONSerializable {
	public JSONObject object(String key) {
		return (JSONObject) computeIfAbsent(key, k -> new JSONObject());
	}

	public JSONArray array(String key) {
		return (JSONArray) computeIfAbsent(key, k -> new JSONArray());
	}

	public String string(String key) {
		return getOrDefault(key, "").toString();
	}

	public Number number(String key) {
		return (Number) getOrDefault(key, 0);
	}

	@Override
	public JsonObject toJson() {
		JsonObject json = new JsonObject();

		for (Entry<String, Object> entry : entrySet()) {
			if (entry.getValue() instanceof JSONSerializable) {
			}
		}

		return json;
	}
}
