package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.data.ContentType;
import dev.gnomebot.app.data.DiscordFeedback;
import dev.gnomebot.app.data.Macro;
import dev.gnomebot.app.server.AppRequest;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.json.JSONObject;
import dev.latvian.apps.json.JSONResponse;
import dev.latvian.apps.tinyserver.http.response.HTTPResponse;
import dev.latvian.apps.tinyserver.http.response.error.client.BadRequestError;
import dev.latvian.apps.tinyserver.http.response.error.client.ForbiddenError;
import dev.latvian.apps.tinyserver.http.response.error.client.NotFoundError;
import dev.latvian.apps.webutils.data.HexId32;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GuildHandlers {
	public static HTTPResponse guildList(AppRequest req) {
		req.checkLoggedIn();

		var futures = new ArrayList<CompletableFuture<PanelGuildData>>();

		for (var gc : req.app.db.allGuilds()) {
			futures.add(CompletableFuture.supplyAsync(() -> {
				var authLevel = gc.getAuthLevel(req.token.userId);

				if (authLevel.is(AuthLevel.MEMBER)) {
					return new PanelGuildData(gc, authLevel);
				} else {
					return null;
				}
			}));
		}

		CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
		var guilds = futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).sorted().toList();

		var root = req.createRoot("Gnome Panel");

		for (var data : guilds) {
			var line = root.content.p().classes("withicon");
			line.img(data.gc().apiUrl() + "/icon/128").lazyLoading();
			line.a(data.gc().url(), data.gc().toString());
		}

		return root.asResponse();
	}

	public static HTTPResponse guild(AppRequest req) {
		req.checkMember();

		var root = req.createRoot(req.gc.toString());
		root.content.h3().a(req.gc.url() + "/audit-log", "Audit Log");
		root.content.h3().a(req.gc.url() + "/macros", "Macros");
		root.content.h3().a(req.gc.url() + "/bans", "Bans");
		root.content.h3().a(req.gc.url() + "/mutes", "Mutes");
		root.content.h3().a(req.gc.url() + "/message-log", "Message Log");
		root.content.h3().a(req.gc.url() + "/voice-log", "Voice Log");
		root.content.h3().a(req.gc.url() + "/reaction-log", "Reaction Log");
		root.content.h3().a(req.gc.url() + "/command-log", "Command Log");
		// root.content.p().string("Uh... nothing for now...");
		// root.content.p().a(req.gc.url()).string("For now you can go to old page.");
		return root.asResponse();
	}

	public static HTTPResponse macros(AppRequest req) {
		req.checkLoggedIn();

		var root = req.createRoot("Macros - " + req.gc);
		root.content.a(req.gc.url(), "< Back").classes("back");

		var slashMacros = root.content.section("macros-slash").classes("divborder").div().h3().string("Macros with Slash Command").end().ol();

		var author = SnowFlake.num(req.query("author").asString());
		var macros = req.gc.getMacroMap().values().stream().filter(m -> author == 0L || m.author == author).sorted().toList();

		var guildCommands = req.gc.db.app.discordHandler.client.getRestClient().getApplicationService().getGuildApplicationCommands(req.gc.db.app.discordHandler.selfId, req.gc.guildId)
				.toStream()
				.collect(Collectors.toMap(d -> d.id().asLong(), Function.identity()));

		for (var macro : macros) {
			if (macro.slashCommand != 0L) {
				try {
					var cmd = guildCommands.get(macro.slashCommand);

					if (cmd == null || !cmd.name().equals(macro.stringId)) {
						throw new NullPointerException();
					}

					slashMacros.li().add(macro);
				} catch (Exception ex) {
					macro.acceptTag0(slashMacros.li()).classes("broken");
				}
			}
		}

		var allMacros = root.content.section("macros").classes("divborder").div().h3().string("All Macros").end().ol();

		for (var macro : macros) {
			allMacros.li().add(macro);
		}

		return root.asResponse();
	}

	public static HTTPResponse macroInfo(AppRequest req) {
		req.checkLoggedIn();

		var macro = req.gc.db.allMacros.get(HexId32.of(req.variable("id")).getAsInt());

		if (macro == null || macro.gc != req.gc) {
			throw new NotFoundError("Macro '" + req.variable("id") + "' not found!");
		}

		if (req.query().containsKey("slash")) {
			if (!req.authLevel().isAdmin()) {
				throw new ForbiddenError("You must be an admin to toggle slash commands");
			}

			macro.setSlashCommand(req.query("slash").asString("0").equals("1"));
			return HTTPResponse.redirect("/" + req.path());
		}

		var root = req.createRoot(macro.name + " - Macros - " + req.gc);
		root.content.a(req.gc.url() + "/macros", "< Back").classes("back");

		var authorId = macro.author;

		var table = root.content.section("info").table().tbody();
		GuildAPIHandlers.member(table.tr().td().string("Author").end().td(), req.gc, authorId);

		if (macro.created == null) {
			table.tr().td().string("Created").end().td().string("Unknown");
		} else {
			table.tr().td().string("Created").end().td().time(macro.created).string(macro.created.toString());
		}

		table.tr().td().string("Type").end().td().string(macro.getCachedContent().a().name);
		table.tr().td().a(macro.gc.url() + "/command-log?command=" + macro.id, "Uses").end().td().string(macro.getUses());

		if (macro.slashCommand == 0L) {
			table.tr().td().string("Slash Command").end().td().string("Disabled").space().a("?slash=1", "(Enable)");
		} else {
			table.tr().td().string("Slash Command").end().td().string("Enabled").space().a("?slash=0", "(Disable)");
			table.tr().td().string("Slash Command ID").end().td().string(Long.toUnsignedString(macro.slashCommand));
		}

		var form = root.content.section("edit").classes("divborder").div().form("POST");
		boolean canEdit = macro.author != 0L && macro.author == req.selfId() || req.authLevel().isAdmin();

		if (canEdit) {
			form.label("name", "Name");
			form.br();
			form.input("text", "name").attr("required").attr("minlength", "1").attr("maxlength", "50").attr("value", macro.name);
			form.br();

			form.label("display-name", "Display Name");
			form.br();
			form.input("text", "display-name").attr("value", macro.displayName);
			form.br();

			form.label("emoji", "Emoji");
			form.br();
			form.input("text", "emoji").attr("value", Utils.reactionToString(macro.emoji));
			form.br();
		}

		form.label("content", "Content");
		form.br();
		var textarea = form.paired("textarea").id(form.getPrefix() + "content").attr("name", "content").attr("required").attr("rows", "20").attr("minlength", "1").string(ContentType.encodeMentions(macro.getContent()));

		if (canEdit) {
			form.br();
			form.input("submit").attr("value", "Save");
		} else {
			textarea.attr("readonly");
		}

		return root.asResponse();
	}

	public static HTTPResponse macroEditInfo(AppRequest req) {
		req.checkMember();

		var macro = req.gc.db.allMacros.get(HexId32.of(req.variable("id")).getAsInt());

		if ((macro.author == 0L || macro.author != req.selfId()) && !req.authLevel().isAdmin()) {
			throw new ForbiddenError("You can't edit this macro!");
		}

		var rename = req.formData("name").asString(macro.name);

		if (!rename.equals(macro.name)) {
			if (rename.isEmpty()) {
				throw new BadRequestError("Macro can't be empty!");
			} else if (rename.length() > 50) {
				throw new BadRequestError("Macro name too long! Max 50 characters.");
			} else if (macro.gc.macroExists(rename)) {
				// ConflictError
				throw new BadRequestError("Macro with that name already exists!");
			}

			macro.rename(rename);
		}

		var displayName = req.formData("display-name").asString(macro.displayName);

		if (!displayName.equals(macro.displayName)) {
			macro.displayName = displayName;
		}

		var emoji = req.formData("emoji").asString(Utils.reactionToString(macro.emoji));

		if (!emoji.equals(Utils.reactionToString(macro.emoji))) {
			macro.emoji = Utils.stringToReaction(emoji);
		}

		macro.setContent(ContentType.decodeMentions(req.formData("content").asString(macro.getContent())));
		macro.gc.saveMacroMap();
		return HTTPResponse.redirect(macro.url());
	}

	public static HTTPResponse memberInfo(AppRequest req) {
		req.checkLoggedIn();

		var memberId = req.getSnowflake("id");
		var user = req.gc.db.app.discordHandler.getUser(memberId);

		if (user == null) {
			throw new NotFoundError("User '" + memberId + "' not found!");
		}

		var member = req.gc.getMember(memberId);
		var globalName = user.getGlobalName().orElse(user.getUsername());
		var name = member == null ? globalName : member.getDisplayName();

		var root = req.createRoot(name + " - Members - " + req.gc);
		root.content.a(req.gc.url(), "< Back").classes("back");

		var info = root.content.div("divwithborder");
		info.div("spread").strong().string("ID").end().spanstr(memberId);
		info.div("spread").strong().string("Username").end().spanstr(user.getUsername());
		info.div("spread").strong().string("Global Name").end().spanstr(globalName);

		if (user.getDiscriminator() != null && !user.getDiscriminator().equals("0")) {
			info.div("spread").strong().string("Tag").end().spanstr(user.getTag());
		}

		if (user.isBot()) {
			info.div("spread").strong().string("Bot").end().spanstr("Yes");
		}

		if (member == null) {
			info.div("spread").strong().string("Member").end().spanstr("No");

			try {
				var ban = req.gc.getGuild().getBan(SnowFlake.convert(memberId)).block();

				if (ban != null) {
					var btag = root.content.div("divwithborder");
					btag.h3().string("⚠️ Banned!");
					btag.p().string(ban.getReason().orElse("Reason Unknown"));
				}
			} catch (Exception ignore) {
			}
		} else {
			var dn = member.getDisplayName();

			if (!dn.equals(globalName)) {
				info.div("spread").strong().string("Nickname").end().spanstr(dn);
			}
		}

		var macros = new ArrayList<Macro>();

		for (var macro : req.gc.getMacroMap().values()) {
			if (macro.author == memberId) {
				macros.add(macro);
			}
		}

		if (!macros.isEmpty()) {
			var btag = root.content.div("divwithborder");
			btag.h3().string("Macros");
			var ul = btag.ul();

			for (var macro : macros.stream().sorted().toList()) {
				ul.li().add(macro);
			}
		}

		return root.asResponse();
	}

	public static HTTPResponse feedbackList(AppRequest req) {
		req.checkMember();

		var list = req.gc.feedback.query()
				.toStream()
				.sorted((o1, o2) -> Integer.compare(o2.getNumber(), o1.getNumber()))
				.toList();

		var memberCache = req.gc.createMemberCache();

		var object = JSONObject.of();
		object.put("id", SnowFlake.str(req.gc.guildId));
		object.put("name", req.gc.name);
		var array = object.addArray("feedback");
		var canSee = DiscordFeedback.canSee(req.gc, req.authLevel());
		var owner = req.authLevel().isOwner();

		for (var feedback : list) {
			feedback.toJson(array.addObject(), memberCache, canSee, owner);
		}

		return JSONResponse.of(object);
	}

	public static HTTPResponse feedback(AppRequest req) throws Exception {
		req.checkMember();

		var id = req.variable("id").asInt();
		var feedback = req.gc.feedback.query().eq("number", id).first();

		if (feedback == null) {
			throw new NotFoundError("Feedback not found!");
		}

		var memberCache = req.gc.createMemberCache();
		var json = JSONObject.of();
		var canSee = DiscordFeedback.canSee(req.gc, req.authLevel());
		feedback.toJson(json, memberCache, canSee, req.authLevel().isOwner());
		return JSONResponse.of(json);
	}

	public static HTTPResponse appeal(AppRequest req) {
		var root = req.createRoot("Appeals - " + req.gc);
		root.content.a(req.gc.url(), "< Back").classes("back");
		root.content.h3().string("Unfortunately, there currently isn't a better appeal process.");
		root.content.h3().string("If you are banned, join the server with an alt and message a moderator.");

		if (!req.gc.getInviteUrl().isEmpty()) {
			root.content.h3().a(req.gc.getInviteUrl(), "Click here").end().string(" to join the server.");
		}

		return root.asResponse();
	}
}
