package dev.gnomebot.app.data;

import dev.gnomebot.app.discord.DiscordHandler;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.MapWrapper;
import discord4j.common.util.Snowflake;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class WebToken extends WrappedDocument<WebToken> {
	public final String token;
	public final Snowflake userId;
	public AuthLevel authLevel = AuthLevel.LOGGED_IN;
	public boolean justLoggedIn = false;

	public WebToken(WrappedCollection<WebToken> c, MapWrapper d) {
		super(c, d);
		token = document.getString("_id");
		userId = Snowflake.of(document.getLong("user"));
	}

	public List<Long> getGuildIds(DiscordHandler handler) {
		List<Long> ids = document.getList("guilds");

		if (ids == null || ids.isEmpty()) {
			ids = new ArrayList<>();

			for (Snowflake guildId : handler.getSelfGuildIds()) {
				GuildCollections gc = handler.app.db.guild(guildId);
				AuthLevel authLevel = gc.getAuthLevel(userId);

				if (authLevel.is(AuthLevel.MEMBER)) {
					ids.add(guildId.asLong());
				}
			}

			update("guilds", ids);
		}

		return ids;
	}
}