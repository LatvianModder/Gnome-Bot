package dev.gnomebot.app.server.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerPathHandler;
import dev.gnomebot.app.server.ServerRequest;
import dev.latvian.apps.webutils.gson.GsonUtils;
import dev.latvian.apps.webutils.net.Response;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface JsonServerPathHandler extends ServerPathHandler {
	Response handleJson(JsonRequest request) throws Exception;

	@Override
	default Response handle(ServerRequest request) throws Exception {
		String body = request.getMainBody().getText();

		try {
			JsonObject in = body.isEmpty() ? null : GsonUtils.GSON.fromJson(body, JsonObject.class);
			return handleJson(new JsonRequest(request, in));
		} catch (JsonIOException | JsonSyntaxException e) {
			throw HTTPResponseCode.BAD_REQUEST.error("Expected JSON Object, got " + body);
		} catch (MissingValueException | WrongJsonTypeException ex) {
			throw HTTPResponseCode.BAD_REQUEST.error(ex.toString());
		}
	}

	@Nullable
	static Object convert(@Nullable JsonElement json) {
		if (json == null || json.isJsonNull()) {
			return null;
		} else if (json.isJsonPrimitive()) {
			JsonPrimitive p = json.getAsJsonPrimitive();

			if (p.isBoolean()) {
				return p.getAsBoolean();
			} else if (p.isNumber()) {
				Number n = p.getAsNumber();

				if (n instanceof Double || n instanceof Float) {
					return n.doubleValue();
				} else if (n instanceof Long) {
					return n.longValue();
				}

				return n.intValue();
			}

			return p.getAsString();
		} else if (json.isJsonArray()) {
			BasicDBList list = new BasicDBList();

			for (JsonElement e : json.getAsJsonArray()) {
				Object o = convert(e);

				if (o != null) {
					list.add(o);
				}
			}

			return list;
		}

		BasicDBObject object = new BasicDBObject();

		for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
			Object o = convert(entry.getValue());

			if (o != null) {
				object.put(entry.getKey(), o);
			}
		}

		return object;
	}
}