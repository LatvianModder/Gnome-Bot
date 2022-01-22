package dev.gnomebot.app.server.json;

import java.util.ArrayList;

/**
 * @author LatvianModder
 */
public class JSONArray extends ArrayList<Object> {
	public JSONObject object(int index) {
		return (JSONObject) get(index);
	}

	public JSONArray array(int index) {
		return (JSONArray) get(index);
	}

	public String string(int index) {
		return get(index).toString();
	}

	public Number number(int index) {
		return (Number) get(index);
	}
}
