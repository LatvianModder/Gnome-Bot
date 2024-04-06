package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.data.ContentType;
import dev.gnomebot.app.data.DiscordFeedback;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.server.GnomeRootTag;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.webutils.data.HexId32;
import dev.latvian.apps.webutils.json.JSONObject;
import dev.latvian.apps.webutils.json.JSONResponse;
import dev.latvian.apps.webutils.net.Response;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.NotFoundResponse;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GuildHandlers {
	public static Response guildList(ServerRequest request) {
		var futures = new ArrayList<CompletableFuture<PanelGuildData>>();

		for (var gc : request.app.db.allGuilds()) {
			futures.add(CompletableFuture.supplyAsync(() -> {
				var authLevel = gc.getAuthLevel(request.token.userId);

				if (authLevel.is(AuthLevel.MEMBER)) {
					return new PanelGuildData(gc, authLevel);
				} else {
					return null;
				}
			}));
		}

		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
		var guilds = futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).sorted().toList();

		var root = GnomeRootTag.createSimple(request.getPath(), "Gnome Panel");

		for (var data : guilds) {
			var line = root.content.p().classes("withicon");
			line.img("/api/guild/icon/" + data.gc().guildId + "/128").lazyLoading();
			line.a("/guild/" + data.gc().guildId).string(data.gc().toString());
		}

		return root.asResponse();
	}

	public static Response guild(ServerRequest request) {
		var root = GnomeRootTag.createSimple(request.getPath(), request.gc.toString());
		root.content.h3().a("/guild/" + request.gc.guildId + "/audit-log", "Audit Log");
		root.content.h3().a("/guild/" + request.gc.guildId + "/macros", "Macros");
		root.content.h3().a("/guild/" + request.gc.guildId + "/bans", "Bans");
		root.content.h3().a("/guild/" + request.gc.guildId + "/mutes", "Mutes");
		root.content.h3().a("/guild/" + request.gc.guildId + "/message-log", "Message Log");
		root.content.h3().a("/guild/" + request.gc.guildId + "/voice-log", "Voice Log");
		root.content.h3().a("/guild/" + request.gc.guildId + "/reaction-log", "Reaction Log");
		// root.content.p().string("Uh... nothing for now...");
		// root.content.p().a("/guild/" + request.gc.guildId.asString()).string("For now you can go to old page.");
		return root.asResponse();
	}

	public static Response macros(ServerRequest request) {
		var root = GnomeRootTag.createSimple(request.getPath(), "Macros - " + request.gc);
		root.content.a("/guild/" + request.gc.guildId, "< Back").classes("back");

		var slashMacros = root.content.section("macros-slash").classes("divborder").div().h3().string("Macros with Slash Command").end().ol();

		var author = request.query("author").asSnowflake();
		var macros = request.gc.getMacroMap().values().stream().filter(m -> author == 0L || m.author == author).sorted().toList();

		var guildCommands = request.gc.db.app.discordHandler.client.getRestClient().getApplicationService().getGuildApplicationCommands(request.gc.db.app.discordHandler.selfId, request.gc.guildId)
				.toStream()
				.collect(Collectors.toMap(d -> d.id().asLong(), Function.identity()));

		for (var macro : macros) {
			if (macro.slashCommand != 0L) {
				try {
					var cmd = guildCommands.get(macro.slashCommand);

					if (cmd == null || !cmd.name().equals(macro.stringId)) {
						throw new NullPointerException();
					}

					slashMacros.li().a("/guild/" + request.gc.guildId + "/macros/" + macro.id, macro.name);
				} catch (Exception ex) {
					slashMacros.li().a("/guild/" + request.gc.guildId + "/macros/" + macro.id, macro.name + " (⚠️ Broken!)").classes("");
				}
			}
		}

		var allMacros = root.content.section("macros").classes("divborder").div().h3().string("All Macros").end().ol();

		for (var macro : macros) {
			allMacros.li().a("/guild/" + request.gc.guildId + "/macros/" + macro.id, macro.name);
		}

		return root.asResponse();
	}

	public static Response macroInfo(ServerRequest request) {
		var macro = request.gc.db.allMacros.get(HexId32.of(request.variable("id")).getAsInt());

		if (macro == null || macro.guild != request.gc) {
			throw new NotFoundResponse("Macro '" + request.variable("id") + "' not found!");
		}

		if (request.hasQuery("slash")) {
			if (!request.getAuthLevel().is(AuthLevel.ADMIN)) {
				throw new ForbiddenResponse("You must be an admin to toggle slash commands");
			}

			macro.setSlashCommand(request.query("slash").asString("0").equals("1"));
			return Response.redirect(request.context.path());
		}

		var root = GnomeRootTag.createSimple(request.getPath(), macro.name + " - Macros - " + request.gc);
		root.content.a("/guild/" + request.gc.guildId + "/macros", "< Back").classes("back");

		var authorId = macro.author;
		var author = request.gc.getMember(authorId);
		var authorName = author == null ? "" : author.getDisplayName();

		if (authorName.isEmpty()) {
			var user = request.gc.db.app.discordHandler.getUser(authorId);

			if (user != null) {
				authorName = user.getGlobalName().orElse(user.getUsername());
			}
		}

		var table = root.content.section("info").table().tbody();
		table.tr().td().string("Author").end().td().a("/guild/" + request.gc.guildId + "/members/" + authorId, authorName);

		if (macro.created == null) {
			table.tr().td().string("Created").end().td().string("Unknown");
		} else {
			table.tr().td().string("Created").end().td().time(macro.created).string(macro.created.toString());
		}

		table.tr().td().string("Uses").end().td().string(macro.getUses());

		if (macro.slashCommand == 0L) {
			table.tr().td().string("Slash Command").end().td().string("Disabled").space().a("?slash=1", "(Enable)");
		} else {
			table.tr().td().string("Slash Command").end().td().string("Enabled").space().a("?slash=0", "(Disable)");
			table.tr().td().string("Slash Command ID").end().td().string(Long.toUnsignedString(macro.slashCommand));
		}

		var newlinePattern = Pattern.compile("\n");

		root.content.h2().string("Content");
		var tag2 = root.content.section("content").classes("divborder").div().p().string(ContentType.encodeMentions(macro.getContent()));
		tag2.replace(newlinePattern, (tag1, matcher) -> tag1.br());

		return root.asResponse();
	}

	public static Response memberInfo(ServerRequest request) {
		var memberId = request.getSnowflake("id");
		var user = request.gc.db.app.discordHandler.getUser(memberId);

		if (user == null) {
			throw new NotFoundResponse("User '" + memberId + "' not found!");
		}

		var member = request.gc.getMember(memberId);
		var name = member == null ? user.getGlobalName().orElse(user.getUsername()) : member.getDisplayName();

		var root = GnomeRootTag.createSimple(request.getPath(), name + " - Members - " + request.gc);
		root.content.a("/guild/" + request.gc.guildId, "< Back").classes("back");
		root.content.h3().string("ID");
		root.content.p().string(memberId);

		if (member == null) {
			try {
				var ban = request.gc.getGuild().getBan(SnowFlake.convert(memberId)).block();

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

	public static Response feedbackList(ServerRequest request) {
		var list = request.gc.feedback.query()
				.toStream()
				.sorted((o1, o2) -> Integer.compare(o2.getNumber(), o1.getNumber()))
				.toList();

		var memberCache = request.gc.createMemberCache();

		var object = JSONObject.of();
		object.put("id", SnowFlake.str(request.gc.guildId));
		object.put("name", request.gc.name);
		var array = object.addArray("feedback");
		var canSee = DiscordFeedback.canSee(request.gc, request.getAuthLevel());
		var owner = request.getAuthLevel().is(AuthLevel.OWNER);

		for (var feedback : list) {
			feedback.toJson(array.addObject(), memberCache, canSee, owner);
		}

		return JSONResponse.of(object);
	}

	public static Response feedback(ServerRequest request) throws Exception {
		var id = (int) request.getUnsignedLong("id");
		var feedback = request.gc.feedback.query().eq("number", id).first();

		if (feedback == null) {
			throw HTTPResponseCode.NOT_FOUND.error("Feedback not found!");
		}

		var memberCache = request.gc.createMemberCache();
		var json = JSONObject.of();
		var canSee = DiscordFeedback.canSee(request.gc, request.getAuthLevel());
		feedback.toJson(json, memberCache, canSee, request.getAuthLevel().is(AuthLevel.OWNER));
		return JSONResponse.of(json);
	}

	public static Response appeal(ServerRequest request) {
		var root = GnomeRootTag.createSimple(request.getPath(), "Appeals - " + request.gc);
		root.content.a("/guild/" + request.gc.guildId, "< Back").classes("back");
		root.content.h3().string("Unfortunately, there currently isn't a better appeal process.");
		root.content.h3().string("If you are banned, join the server with an alt and message a moderator.");

		if (!request.gc.getInviteUrl().isEmpty()) {
			root.content.h3().a(request.gc.getInviteUrl(), "Click here").end().string(" to join the server.");
		}

		return root.asResponse();
	}
}
