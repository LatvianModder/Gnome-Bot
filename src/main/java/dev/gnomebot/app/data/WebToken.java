package dev.gnomebot.app.data;

import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.MapWrapper;

public class WebToken extends WrappedDocument<WebToken> {
	public final String token;
	public final long userId;
	public AuthLevel authLevel = AuthLevel.LOGGED_IN;
	public boolean justLoggedIn = false;

	public WebToken(WrappedCollection<WebToken> c, MapWrapper d) {
		super(c, d);
		token = document.getString("_id");
		userId = document.getLong("user");
	}
}