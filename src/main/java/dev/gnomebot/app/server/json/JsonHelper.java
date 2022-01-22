package dev.gnomebot.app.server.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * @author LatvianModder
 */
public class JsonHelper {
	public static JsonObject singleJson(String key, JsonElement value) {
		JsonObject json = new JsonObject();
		json.add(key, value);
		return json;
	}

	public static JsonObject singleJson(String key, String value) {
		return singleJson(key, new JsonPrimitive(value));
	}

	public static JsonObject error(String error) {
		return singleJson("error", error);
	}
}