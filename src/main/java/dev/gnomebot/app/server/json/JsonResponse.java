package dev.gnomebot.app.server.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.gnomebot.app.server.handler.Response;
import dev.gnomebot.app.util.Utils;
import io.javalin.http.Context;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class JsonResponse implements Response {
	public static JsonResponse object(final Consumer<JsonObject> consumer) {
		return new JsonResponse(() -> {
			JsonObject object = new JsonObject();
			consumer.accept(object);
			return object;
		});
	}

	public static JsonResponse array(final Consumer<JsonArray> consumer) {
		return new JsonResponse(() -> {
			JsonArray array = new JsonArray();
			consumer.accept(array);
			return array;
		});
	}

	public static JsonResponse array(JsonArray array) {
		return new JsonResponse(() -> array);
	}

	public final Supplier<JsonElement> json;
	private String jsonString = null;

	private JsonResponse(Supplier<JsonElement> e) {
		json = e;
	}

	@Override
	public void result(Context ctx) {
		ctx.status(200);
		ctx.contentType("application/json; charset=utf-8");
		ctx.future(() -> CompletableFuture.supplyAsync(this::getJsonString));
	}

	public String getJsonString() {
		if (jsonString == null) {
			jsonString = Utils.GSON.toJson(json.get());
		}

		return jsonString;
	}
}