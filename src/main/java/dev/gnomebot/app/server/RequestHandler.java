package dev.gnomebot.app.server;

import dev.gnomebot.app.App;
import io.javalin.http.HandlerType;

import java.util.Map;

/**
 * @author LatvianModder
 */
public class RequestHandler {
	public final App app;
	public final String path;
	public final String[] splitPath;
	public final ServerPathHandler handler;

	public HandlerType method;
	public int cacheSeconds;
	public AuthLevel authLevel;
	public boolean trusted;
	public boolean log;

	public RequestHandler(App m, String p, ServerPathHandler h) {
		app = m;

		while (p.startsWith("/")) {
			p = p.substring(1);
		}

		while (p.endsWith("/")) {
			p = p.substring(0, p.length() - 1);
		}

		path = p;
		splitPath = path.split("/");
		handler = h;

		method = HandlerType.GET;
		cacheSeconds = 0;
		authLevel = AuthLevel.LOGGED_IN;
		trusted = false;
		log = false;
	}

	public RequestHandler method(HandlerType m) {
		method = m;
		return this;
	}

	public RequestHandler post() {
		return method(HandlerType.POST);
	}

	public RequestHandler patch() {
		return method(HandlerType.PATCH);
	}

	public RequestHandler delete() {
		return method(HandlerType.DELETE);
	}

	public RequestHandler put() {
		return method(HandlerType.PUT);
	}

	public RequestHandler cacheSeconds(int seconds) {
		cacheSeconds = seconds;
		return this;
	}

	public RequestHandler cacheMinutes(int minutes) {
		return cacheSeconds(minutes * 60);
	}

	public RequestHandler cacheHours(int hours) {
		return cacheMinutes(hours * 60);
	}

	public RequestHandler cacheDays(int days) {
		return cacheHours(days * 24);
	}

	public RequestHandler auth(AuthLevel level) {
		authLevel = level;
		return this;
	}

	public RequestHandler noAuth() {
		return auth(AuthLevel.NO_AUTH);
	}

	public RequestHandler member() {
		return auth(AuthLevel.MEMBER);
	}

	public RequestHandler admin() {
		return auth(AuthLevel.ADMIN);
	}

	public RequestHandler owner() {
		return auth(AuthLevel.OWNER);
	}

	public RequestHandler trusted() {
		trusted = true;
		return this;
	}

	public RequestHandler log() {
		log = true;
		return this;
	}

	public String toString() {
		return path;
	}

	public boolean matches(String[] p, Map<String, String> vars) {
		if (splitPath.length != p.length) {
			return false;
		}

		for (int i = 0; i < splitPath.length; i++) {
			if (splitPath[i].charAt(0) == ':') {
				vars.put(splitPath[i].substring(1), p[i]);
			} else if (!splitPath[i].equals(p[i])) {
				return false;
			}
		}

		return true;
	}
}