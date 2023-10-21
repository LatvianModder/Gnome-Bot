package dev.gnomebot.app.server.handler.panel;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.server.GnomeRootTag;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.server.handler.PanelGuildData;
import dev.latvian.apps.webutils.net.Response;
import discord4j.common.util.Snowflake;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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

		var root = GnomeRootTag.createSimple(request.getPath(), "Gnome Panel");

		for (PanelGuildData data : guilds) {
			var line = root.content.p().classes("withicon");
			line.img("/api/guild/icon/" + data.id().asString() + "/128");
			line.a("/panel/" + data.id().asString()).string(data.name());
		}

		return root.asResponse();
	}

	public static Response login(ServerRequest request) {
		if (!request.query("logintoken").isPresent()) {
			var root = GnomeRootTag.createSimple(request.getPath(), "Gnome Panel");

			if (request.token == null) {
				root.content.p().string("What the heck? You shouldn't be here, shoo!");
			} else {
				root.content.p().string("You've successfully logged in, " + request.token.getName() + "!");
				root.content.p().string("You can now close this page.");
				root.content.p().a("/panel").string("You can click here to browse guild list.");
			}

			return root.asResponse();
		}

		return Response.redirect(App.url("panel/login"));
	}

	public static Response guild(ServerRequest request) {
		var root = GnomeRootTag.createSimple(request.getPath(), request.gc + " - Gnome Panel");
		root.content.p().string("Uh... nothing for now...");
		root.content.p().a("/guild/" + request.gc.guildId.asString()).string("For now you can go to old page.");
		return root.asResponse();
	}

	public static Response macros(ServerRequest request) {
		var root = GnomeRootTag.createSimple(request.getPath(), "Macros - " + request.gc + " - Gnome Panel");

		var macros = root.content.section("macros").classes("divborder");

		var newlinePattern = Pattern.compile("\n");
		var boldPattern = Pattern.compile("\\*\\*(.*?)\\*\\*");
		var underlinedPattern = Pattern.compile("__(.*?)__");
		var italicPattern = Pattern.compile("([*_])(.*?)\\1");

		for (var macro : request.gc.getMacroMap().values()) {
			var div = macros.div();
			var author = request.gc.getMember(Snowflake.of(macro.getAuthor()));
			div.p().span("yellow", macro.getName()).end().space().string("by").space().span("blue", author == null ? Snowflake.of(macro.getAuthor()).asString() : author.getDisplayName());
			var tag = div.p().string(macro.getContent());
			tag.replace(newlinePattern, (tag1, matcher) -> tag1.br());
			tag.replace(boldPattern, (tag1, matcher) -> tag1.paired("strong").string(matcher.group(1)));
			tag.replace(underlinedPattern, (tag1, matcher) -> tag1.paired("u").string(matcher.group(1)));
			tag.replace(italicPattern, (tag1, matcher) -> tag1.paired("em").string(matcher.group(2)));

			if (!macro.getExtra().isEmpty()) {
				var ul = div.ul();

				for (var e : macro.getExtra()) {
					ul.li().string(e);
				}
			}

			// var line = content.p().classes("withicon");
			// line.img("/api/guild/icon/" + request.gc.guildId.asString() + "/128");
			// line.a("/panel/" + request.gc.guildId.asString() + "/macros/" + macro.id).string(macro.name);
		}

		return root.asResponse();
	}
}