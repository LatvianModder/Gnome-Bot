package dev.gnomebot.app.server.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.gnomebot.app.server.ServerRequest;
import org.jetbrains.annotations.Nullable;

/**
 * @author LatvianModder
 */
public class JsonRequest {
	public final ServerRequest request;
	public final JsonObject json;

	public JsonRequest(ServerRequest d, @Nullable JsonObject in) {
		request = d;
		json = in;
	}

	public boolean has(String key) {
		return json != null && json.has(key);
	}

	public JsonElement get(String key) {
		JsonElement e = json == null ? null : json.get(key);

		if (e != null && !e.isJsonNull()) {
			return e;
		}

		throw new MissingValueException(key);
	}

	public String getString(String key) throws NotJsonNumberException {
		JsonElement e = get(key);

		if (e.isJsonPrimitive()) {
			return e.getAsString();
		}

		throw new NotJsonStringException();
	}

	public Number getNumber(String key) throws NotJsonNumberException {
		JsonElement e = get(key);

		if (e.isJsonPrimitive()) {
			return e.getAsNumber();
		}

		throw new NotJsonNumberException();
	}
}