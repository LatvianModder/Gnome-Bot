package dev.gnomebot.app.server.handler.panel;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.ContentType;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.server.GnomeRootTag;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.server.handler.PanelGuildData;
import dev.latvian.apps.webutils.CodingUtils;
import dev.latvian.apps.webutils.net.Response;
import discord4j.common.util.Snowflake;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.NotFoundResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PanelHandlers {
	public static Response root(ServerRequest request) {
		List<PanelGuildData> guilds = new ArrayList<>();

		for (long guildId0 : request.token.getGuildIds(request.app.discordHandler)) {
			Snowflake guildId = Snowflake.of(guildId0);
			GuildCollections gc = request.app.db.guild(guildId);
			AuthLevel authLevel = gc.getAuthLevel(request.token.userId);

			if (authLevel.is(AuthLevel.MEMBER)) {
				guilds.add(new PanelGuildData(guildId, gc.toString(), gc.ownerId, authLevel));
			}
		}

		guilds.sort((o1, o2) -> o1.name().compareToIgnoreCase(o2.name()));

		var root = GnomeRootTag.createSimple(request.getPath(), "Gnome Panel");

		for (PanelGuildData data : guilds) {
			var line = root.content.p().classes("withicon");
			line.img("/api/guild/icon/" + data.id().asString() + "/128").lazyLoading();
			line.a("/panel/" + data.id().asString()).string(data.name());
		}

		return root.asResponse();
	}

	public static Response login(ServerRequest request) {
		if (!request.query("logintoken").isPresent()) {
			var root = GnomeRootTag.createSimple(request.getPath(), "Logged In");

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
		var root = GnomeRootTag.createSimple(request.getPath(), request.gc.toString());
		root.content.h3().a("/panel/" + request.gc.guildId.asString() + "/audit-log?level=1", "Audit Log");
		root.content.h3().a("/panel/" + request.gc.guildId.asString() + "/audit-log?level=0&type=message_edited,message_deleted", "Audit Log (Deleted/Edited Messages)");
		root.content.h3().a("/panel/" + request.gc.guildId.asString() + "/macros", "Macros");
		root.content.h3().a("/panel/" + request.gc.guildId.asString() + "/bans", "Bans");
		root.content.h3().a("/panel/" + request.gc.guildId.asString() + "/offenses", "Offenses");
		root.content.h3().a("/panel/" + request.gc.guildId.asString() + "/scam-detection", "Scam Detection");
		// root.content.p().string("Uh... nothing for now...");
		// root.content.p().a("/guild/" + request.gc.guildId.asString()).string("For now you can go to old page.");
		return root.asResponse();
	}

	public static Response macros(ServerRequest request) {
		var root = GnomeRootTag.createSimple(request.getPath(), "Macros - " + request.gc);
		root.content.a("/panel/" + request.gc.guildId.asString(), "< Back").classes("back");

		var slashMacros = root.content.section("macros-slash").classes("divborder").div().h3().string("Macros with Slash Command").end().ol();

		var author = request.query("author").asSnowflake().asLong();
		var macros = request.gc.getMacroMap().values().stream().filter(m -> author == 0L || m.author == author).sorted().toList();

		var guildCommands = request.gc.db.app.discordHandler.client.getRestClient().getApplicationService().getGuildApplicationCommands(request.gc.db.app.discordHandler.applicationId, request.gc.guildId.asLong())
				.toStream()
				.collect(Collectors.toMap(d -> d.id().asLong(), Function.identity()));

		for (var macro : macros) {
			if (macro.slashCommand != 0L) {
				try {
					var cmd = guildCommands.get(macro.slashCommand);

					if (cmd == null || !cmd.name().equals(macro.id)) {
						throw new NullPointerException();
					}

					slashMacros.li().a("/panel/" + request.gc.guildId.asString() + "/macros/" + CodingUtils.encodeURL(macro.id), macro.name);
				} catch (Exception ex) {
					slashMacros.li().a("/panel/" + request.gc.guildId.asString() + "/macros/" + CodingUtils.encodeURL(macro.id), macro.name + " (Broken!)").classes("");
				}
			}
		}

		var allMacros = root.content.section("macros").classes("divborder").div().h3().string("All Macros").end().ol();

		for (var macro : macros) {
			allMacros.li().a("/panel/" + request.gc.guildId.asString() + "/macros/" + CodingUtils.encodeURL(macro.id), macro.name);
		}

		return root.asResponse();
	}

	public static Response macroInfo(ServerRequest request) {
		var macroName = CodingUtils.decodeURL(request.variable("name"));

		var macro = request.gc.getMacro(macroName);

		if (macro == null) {
			throw new NotFoundResponse("Macro '" + macroName + "' not found!");
		}

		if (request.hasQuery("slash")) {
			if (!request.getAuthLevel().is(AuthLevel.ADMIN)) {
				throw new ForbiddenResponse("You must be an admin to toggle slash commands");
			}

			macro.setSlashCommand(request.query("slash").asString("0").equals("1"));
			return Response.redirect(request.context.path());
		}

		var root = GnomeRootTag.createSimple(request.getPath(), macro.name + " - Macros - " + request.gc);
		root.content.a("/panel/" + request.gc.guildId.asString() + "/macros", "< Back").classes("back");

		var authorId = Snowflake.of(macro.author);
		var author = request.gc.getMember(authorId);
		var authorName = author == null ? "" : author.getDisplayName();

		if (authorName.isEmpty()) {
			var user = request.gc.db.app.discordHandler.getUser(authorId);

			if (user != null) {
				authorName = user.getGlobalName().orElse(user.getUsername());
			}
		}

		var table = root.content.section("info").table().tbody();
		table.tr().td().string("Author").end().td().a("/panel/" + request.gc.guildId.asString() + "/members/" + authorId.asString(), authorName);

		if (macro.created == null) {
			table.tr().td().string("Created").end().td().string("Unknown");
		} else {
			table.tr().td().string("Created").end().td().time(macro.created).string(macro.created.toString());
		}

		table.tr().td().string("Uses").end().td().string(macro.uses);

		if (macro.slashCommand == 0L) {
			table.tr().td().string("Slash Command").end().td().string("Disabled").space().a("?slash=1", "(Enable)");
		} else {
			table.tr().td().string("Slash Command").end().td().string("Enabled").space().a("?slash=0", "(Disable)");
			table.tr().td().string("Slash Command ID").end().td().string(Long.toUnsignedString(macro.slashCommand));
		}

		var newlinePattern = Pattern.compile("\n");

		root.content.h2().string("Content");
		var tag2 = root.content.section("content").classes("divborder").div().p().string(ContentType.encodeMentions(macro.content));
		tag2.replace(newlinePattern, (tag1, matcher) -> tag1.br());

		return root.asResponse();
	}

	public static Response memberInfo(ServerRequest request) {
		var memberId = request.getSnowflake("id");
		var user = request.gc.db.app.discordHandler.getUser(memberId);

		if (user == null) {
			throw new NotFoundResponse("User '" + memberId.asString() + "' not found!");
		}

		var member = request.gc.getMember(memberId);
		var name = member == null ? user.getGlobalName().orElse(user.getUsername()) : member.getDisplayName();

		var root = GnomeRootTag.createSimple(request.getPath(), name + " - Members - " + request.gc);
		root.content.a("/panel/" + request.gc.guildId.asString(), "< Back").classes("back");
		root.content.h3().string("ID");
		root.content.p().string(memberId.asString());

		if (member == null) {
			try {
				var ban = request.gc.getGuild().getBan(memberId).block();

				if (ban != null) {
					var btag = root.content.div("divwithborder");
					btag.h3().string("⚠️ Banned!");
					btag.p().string(ban.getReason().orElse("Reason Unknown"));
				}
			} catch (Exception ignore) {
			}
		}

		return root.asResponse();
	}
}