package dev.gnomebot.app.server.json;

import com.google.gson.JsonElement;

/**
 * @author LatvianModder
 */
public interface JSONSerializable {
	JsonElement toJson();
}
