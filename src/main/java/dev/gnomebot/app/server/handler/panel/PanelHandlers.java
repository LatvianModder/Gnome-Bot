package dev.gnomebot.app.server.handler.panel;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.server.handler.PanelGuildData;
import dev.gnomebot.app.server.handler.Redirect;
import dev.gnomebot.app.server.handler.Response;
import dev.gnomebot.app.server.html.RootTag;
import dev.gnomebot.app.server.html.Tag;
import discord4j.common.util.Snowflake;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class PanelHandlers {
	public static Response root(ServerRequest request) {
		List<PanelGuildData> guilds = new ArrayList<>();

		for (long guildId0 : request.token.getGuildIds(request.app.discordHandler)) {
			Snowflake guildId = Snowflake.of(guildId0);
			GuildCollections gc = request.app.db.guild(guildId);
			AuthLevel authLevel = gc.getAuthLevel(request.token.userId);

			if (authLevel.is(AuthLevel.MEMBER)) {
				guilds.add(new PanelGuildData(guildId, gc.toString(), gc.ownerId.get(), authLevel));
			}
		}

		guilds.sort((o1, o2) -> o1.name().compareToIgnoreCase(o2.name()));

		Tag content = RootTag.createSimple(request.getPath(), "Gnome Panel");

		for (PanelGuildData data : guilds) {
			Tag line = content.p().addClass("withicon");
			line.unpaired("img").attr("src", "/api/guild/icon/" + data.id().asString() + "/128");
			line.a("/panel/" + data.id().asString()).string(data.name());
		}

		return content.asResponse();
	}

	public static Response login(ServerRequest request) {
		if (!request.query("logintoken").isPresent()) {
			Tag content = RootTag.createSimple(request.getPath(), "Gnome Panel");

			if (request.token == null) {
				content.p().string("What the heck? You shouldn't be here, shoo!");
			} else {
				content.p().string("You've successfully logged in, " + request.token.getName() + "!");
				content.p().string("You can now close this page.");
				content.p().a("/panel").string("You can click here to browse guild list.");
			}

			return content.asResponse();
		}

		return Redirect.temporarily(App.url("panel/login"));
	}

	public static Response guild(ServerRequest request) {
		Tag content = RootTag.createSimple(request.getPath(), "Gnome Panel - " + request.gc);
		content.p().string("Uh... nothing for now...");
		content.p().a("/guild/" + request.gc.guildId.asString()).string("For now you can go to old page.");
		return content.asResponse();
	}
}