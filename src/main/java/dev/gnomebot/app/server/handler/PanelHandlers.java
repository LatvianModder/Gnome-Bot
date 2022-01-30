package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.server.html.RootTag;
import dev.gnomebot.app.server.html.Tag;
import discord4j.common.util.Snowflake;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class PanelHandlers {
	public static Response guilds(ServerRequest request) {
		List<PanelGuildData> guilds = new ArrayList<>();

		for (Snowflake guildId : request.app.discordHandler.getSelfGuildIds()) {
			GuildCollections gc = request.app.db.guild(guildId);
			AuthLevel authLevel = gc.getAuthLevel(request.token.userId);

			if (authLevel.is(AuthLevel.MEMBER)) {
				guilds.add(new PanelGuildData(guildId, gc.toString(), gc.ownerId.get(), authLevel));
			}
		}

		guilds.sort((o1, o2) -> o1.name().compareToIgnoreCase(o2.name()));

		RootTag root = RootTag.create();
		root.head("Gnome Panel", "panel/guilds");
		Tag body = root.paired("body");
		Tag content = body.div().addClass("content");

		content.h3().string("Gnome Panel");
		content.br();

		for (PanelGuildData data : guilds) {
			Tag line = content.p().addClass("withicon");
			line.unpaired("img").attr("src", "/api/guild/icon/" + data.id().asString() + "/128");
			line.a("/panel/guilds/" + data.id().asString()).string(data.name());
		}

		return root.asResponse();
	}
}