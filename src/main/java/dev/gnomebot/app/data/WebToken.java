package dev.gnomebot.app.data;

import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.MapWrapper;
import discord4j.common.util.Snowflake;

/**
 * @author LatvianModder
 */
public class WebToken extends WrappedDocument<WebToken> {
	public final Snowflake userId;
	public AuthLevel authLevel = AuthLevel.LOGGED_IN;

	public WebToken(WrappedCollection<WebToken> c, MapWrapper d) {
		super(c, d);
		userId = Snowflake.of(document.getLong("user"));
	}
}