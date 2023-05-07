package dev.gnomebot.app.server.json;

import dev.gnomebot.app.server.ServerRequest;
import dev.latvian.apps.webutils.json.JSONObject;
import org.jetbrains.annotations.Nullable;

/**
 * @author LatvianModder
 */
public class JsonRequest {
	public final ServerRequest request;
	public final JSONObject json;

	public JsonRequest(ServerRequest d, @Nullable JSONObject in) {
		request = d;
		json = in;
	}

	public boolean has(String key) {
		return json != null && json.containsKey(key);
	}

	public Object get(String key) {
		var e = json == null ? null : json.get(key);

		if (e != null) {
			return e;
		}

		throw new MissingValueException(key);
	}

	public String getString(String key) throws NotJsonNumberException {
		var e = get(key);

		if (e instanceof String || e instanceof Number || e instanceof Boolean) {
			return String.valueOf(e);
		}

		throw new NotJsonStringException();
	}

	public Number getNumber(String key) throws NotJsonNumberException {
		var e = get(key);

		if (e instanceof Number n) {
			return n;
		}

		throw new NotJsonNumberException();
	}
}