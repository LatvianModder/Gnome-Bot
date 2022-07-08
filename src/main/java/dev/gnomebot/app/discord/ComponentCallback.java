package dev.gnomebot.app.discord;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@FunctionalInterface
public interface ComponentCallback {
	Set<String> NONE = new HashSet<>();
	Set<String> SELF = new HashSet<>();

	/**
	 * @return true if callback should be deleted
	 */
	Set<String> run(ComponentEventWrapper event);

	Map<String, ComponentCallback> MAP = new HashMap<>();

	static String id(ComponentCallback callback) {
		String id = UUID.randomUUID().toString().replace("-", "");
		MAP.put(id, callback);
		return "callback/" + id;
	}
}
