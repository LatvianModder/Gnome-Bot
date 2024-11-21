package dev.gnomebot.app.server;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public enum AuthLevel {
	NO_AUTH("no_auth"),
	LOGGED_IN("logged_in"),
	MEMBER("member"),
	ADMIN("admin"),
	OWNER("owner");

	public static final Map<String, AuthLevel> MAP = Arrays.stream(values()).collect(Collectors.toMap(a -> a.name, a -> a));

	public static AuthLevel get(String s) {
		return Objects.requireNonNull(MAP.get(s));
	}

	public final String name;

	AuthLevel(String n) {
		name = n;
	}

	public boolean is(AuthLevel level) {
		return ordinal() >= level.ordinal();
	}

	public boolean isLoggedIn() {
		return is(LOGGED_IN);
	}

	public boolean isMember() {
		return is(MEMBER);
	}

	public boolean isAdmin() {
		return is(ADMIN);
	}

	public boolean isOwner() {
		return is(OWNER);
	}
}
