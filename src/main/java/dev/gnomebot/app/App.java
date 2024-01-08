package dev.gnomebot.app;

import dev.gnomebot.app.data.Databases;
import dev.gnomebot.app.data.ScheduledTask;
import dev.gnomebot.app.data.ping.PingHandler;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.DiscordHandler;
import dev.gnomebot.app.discord.ReactionHandler;
import dev.gnomebot.app.discord.ScamHandler;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.script.DiscordJS;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.server.RequestHandler;
import dev.gnomebot.app.server.WSHandler;
import dev.gnomebot.app.server.WebServer;
import dev.gnomebot.app.server.handler.ActivityHandlers;
import dev.gnomebot.app.server.handler.GuildHandlers;
import dev.gnomebot.app.server.handler.InfoHandlers;
import dev.gnomebot.app.server.handler.MinecraftHandlers;
import dev.gnomebot.app.server.handler.MiscHandlers;
import dev.gnomebot.app.server.handler.PasteHandlers;
import dev.gnomebot.app.server.handler.SpecialHandlers;
import dev.gnomebot.app.server.handler.panel.AuditLogHandlers;
import dev.gnomebot.app.server.handler.panel.PanelHandlers;
import dev.gnomebot.app.server.handler.panel.ScamWebHandlers;
import dev.gnomebot.app.util.BlockingTask;
import dev.gnomebot.app.util.BlockingTaskCallback;
import dev.gnomebot.app.util.CharMap;
import dev.latvian.apps.webutils.TimeUtils;
import dev.latvian.apps.webutils.ansi.Ansi;
import dev.latvian.apps.webutils.ansi.Table;
import discord4j.common.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;

public class App implements Runnable {
	public static final Instant START_INSTANT = Instant.now();
	public static final Logger LOGGER = new Logger();
	public static boolean debug = false;
	public static App instance;

	public static void main(String[] args) {
		new Thread(instance = new App(), "MainThread").start();
	}

	public static void info(Object message) {
		LOGGER.info(message);
	}

	public static void success(Object message) {
		LOGGER.success(message);
	}

	public static void warn(Object message) {
		LOGGER.warn(message);
	}

	public static void error(Object message) {
		LOGGER.error(message);
	}

	public static void warn(Exception ex) {
		if (ex instanceof ClientException) {
			warn(ex.getMessage());
		} else {
			warn(ex.toString());
		}
	}

	public static String url(String path) {
		if (path.isEmpty()) {
			return Config.get().panel_url;
		}

		return Config.get().panel_url + path;
	}

	public boolean running = true;
	public ConcurrentLinkedDeque<BlockingTask> blockingTasks = new ConcurrentLinkedDeque<>();
	public ConcurrentLinkedDeque<ScheduledTask> scheduledTasks = new ConcurrentLinkedDeque<>();

	public CLI cli;
	public Databases db;
	public WebServer webServer;
	public DiscordHandler discordHandler;
	// TODO: Move more handlers from static instances to here
	public PingHandler pingHandler;

	@Override
	public void run() {
		// LOGGER.replaceSystemOutLogger();
		App.info("Starting up...");
		Locale.setDefault(Locale.US);
		System.setProperty("java.awt.headless", "true");
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");
		System.out.println("Log start: " + Instant.now());

		running = true;

		App.info("Loading char map...");
		CharMap.load();

		cli = new CLI(this);
		cli.start();
		db = new Databases(this);

		App.info("Loading discord handler...");

		discordHandler = new DiscordHandler(this);
		discordHandler.load();
		App.info("Discord handler loaded");

		db.createSelfToken();

		pingHandler = new PingHandler(db);

		App.info("Loading web server...");
		webServer = new WebServer(this);

		webServer.add("api/account/sign-out", MiscHandlers::signOut).post().log();

		webServer.add("api/info/ping", InfoHandlers::ping).noAuth();
		webServer.add("api/info/user/:user", InfoHandlers::user).noAuth().cacheHours(1);
		webServer.add("api/info/avatar/:user/:size", InfoHandlers::avatar).noAuth().cacheDays(1);
		webServer.add("api/info/emoji/:emoji/:size", InfoHandlers::emoji).noAuth().cacheDays(1);
		webServer.add("api/info/define/:word", InfoHandlers::define).noAuth().cacheMinutes(1);
		webServer.add("api/info/video-thumbnail/:channel/:message/:attachment", InfoHandlers::videoThumbnail).noAuth().cacheDays(1);

		webServer.add("api/guilds", GuildHandlers::guilds).cacheMinutes(1).log();
		webServer.add("api/guild/info/:guild", GuildHandlers::info).member().cacheHours(1);
		webServer.add("api/guild/banner/:guild", GuildHandlers::banner).noAuth().cacheSeconds(1);
		webServer.add("api/guild/settings/basic/:guild", GuildHandlers::getSettings).member();
		webServer.add("api/guild/settings/basic/:guild/:key", GuildHandlers::updateSetting).patch().owner();
		webServer.add("api/guild/icon/:guild/:size", GuildHandlers::icon).noAuth().cacheDays(1);
		webServer.add("api/guild/file/:channel/:id/:filename", PasteHandlers::file).noAuth().cacheMinutes(5);
		webServer.add("paste/:id/raw", PasteHandlers::pasteRaw).noAuth().cacheMinutes(5);
		webServer.add("paste/:id", PasteHandlers::paste).noAuth().cacheMinutes(5);
		webServer.add("paste/:channel/:id/:name/:author", PasteHandlers::pasteDirect).noAuth().cacheMinutes(5);
		webServer.add("api/guild/feedback/:guild/:id", GuildHandlers::feedback).member();
		webServer.add("api/guild/feedback/:guild", GuildHandlers::feedbackList).member().cacheMinutes(1);
		webServer.add("api/guild/polls/:guild/:id", GuildHandlers::poll).member();
		webServer.add("api/guild/polls/:guild", GuildHandlers::pollList).member();
		webServer.add("api/guild/members/:guild", GuildHandlers::members).member().cacheMinutes(5);
		webServer.add("api/guild/member/:guild/:member", GuildHandlers::member).member().cacheMinutes(5);
		webServer.add("api/guild/audit-log/:guild", GuildHandlers::auditLog).admin().log();
		webServer.add("api/guild/export-messages/:guild/:member", GuildHandlers::exportMessages).admin().log();

		webServer.add("api/guild/activity/leaderboard/:guild/:days", ActivityHandlers::leaderboard).member().cacheHours(1);
		webServer.add("api/guild/activity/leaderboard-image/:guild/:days", ActivityHandlers::leaderboardImage).member().cacheHours(1);
		webServer.add("api/guild/activity/rank/:guild/:member/:days", ActivityHandlers::rank).member().cacheHours(0); // 1
		webServer.add("api/guild/activity/members/:guild", ActivityHandlers::members).member().cacheMinutes(5);
		webServer.add("api/guild/activity/channels/:guild", ActivityHandlers::channels).member().cacheMinutes(5);
		webServer.add("api/guild/activity/user-mention-leaderboard-image/:guild/:mention/:days", ActivityHandlers::userMentionLeaderboardImage).admin().cacheHours(1);
		webServer.add("api/guild/activity/role-mention-leaderboard-image/:guild/:mention/:days", ActivityHandlers::roleMentionLeaderboardImage).admin().cacheHours(1);

		webServer.add("api/rust-plus/:from", MiscHandlers::rustPlus).post().noAuth().log();

		webServer.add("public/:file", SpecialHandlers::publicfile).noAuth().cacheMinutes(5);

		webServer.add("panel", PanelHandlers::root).log();
		webServer.add("panel/login", PanelHandlers::login).noAuth().log();
		webServer.add("panel/:guild", PanelHandlers::guild).member();
		webServer.add("panel/:guild/audit-log", AuditLogHandlers::auditLog).admin();
		webServer.add("panel/:guild/offenses", AuditLogHandlers::offenses).member();
		webServer.add("panel/:guild/offenses/:user", AuditLogHandlers::offensesOf).admin();
		webServer.add("panel/:guild/bans", AuditLogHandlers::bans).admin();
		webServer.add("panel/:guild/scam-detection", ScamWebHandlers::scamDetection).admin();
		webServer.add("panel/:guild/macros", PanelHandlers::macros);
		webServer.add("panel/:guild/macros/:name", PanelHandlers::macroInfo);
		webServer.add("panel/:guild/members/:id", PanelHandlers::memberInfo);

		webServer.add("minecraft/verify", MinecraftHandlers::verify).noAuth().log();

		webServer.addWS("api/cli", WSHandler.CLI);

		webServer.start(Config.get().port);

		info("");
		info("API Endpoints:");
		info("");

		var table = new Table("Method", "Path", "Auth Level", "Cache");

		for (RequestHandler wrapper : webServer.handlerList) {
			var cells = table.addRow();

			switch (wrapper.method) {
				case GET -> cells[0].value(Ansi.green("GET"));
				case POST -> cells[0].value(Ansi.cyan("POST"));
				case PATCH -> cells[0].value(Ansi.teal("PATCH"));
				case DELETE -> cells[0].value(Ansi.red("DELETE"));
				case PUT -> cells[0].value(Ansi.yellow("PUT"));
			}

			var psb = Ansi.of();

			for (var p : wrapper.path.split("/")) {
				psb.append(Ansi.cyan('/'));

				if (p.startsWith(":")) {
					psb.append(Ansi.yellow(p.substring(1)));
				} else {
					psb.append(p);
				}
			}

			cells[1].value(psb);
			cells[2].value(Ansi.of(wrapper.authLevel.name().toLowerCase()).color(wrapper.authLevel == AuthLevel.NO_AUTH ? 11 : 2));

			if (wrapper.cacheSeconds == 0) {
				cells[3].value(Ansi.yellow("no-cache"));
			} else {
				cells[3].value(Ansi.green((wrapper.authLevel == AuthLevel.NO_AUTH ? "public " : "private ") + TimeUtils.prettyTimeString(wrapper.cacheSeconds)));
			}
		}

		table.print();

		info("");

		ScamHandler.loadDomains();
		DM.loadDmChannels();

		try {
			ApplicationCommands.updateCommands(discordHandler);
		} catch (Exception ex) {
			error("Failed to update commands:");
			ex.printStackTrace();
		}

		printScheduled();

		pingHandler.getPings();

		success("Server restarted!");
		new WatchdogThread(this).start();

		try {
			while (running) {
				if (!blockingTasks.isEmpty()) {
					var task = blockingTasks.removeFirst();
					task.callback.run(task);
				}

				if (!scheduledTasks.isEmpty()) {
					long now = System.currentTimeMillis();

					var itr = scheduledTasks.iterator();

					while (itr.hasNext()) {
						var t = itr.next();

						try {
							if (t.run(now)) {
								itr.remove();
							}
						} catch (Exception ex) {
							ex.printStackTrace();
							itr.remove();
						}
					}
				}

				Thread.sleep(5L);
			}
		} catch (Throwable ex) {
			error("Server crashed!");
			ex.printStackTrace();
		}

		info("Stopping the server...");
		webServer.stop();
		discordHandler.client.logout().block();
		info("Server stopped!");
		System.exit(0);
	}

	public void queueBlockingTask(BlockingTaskCallback task) {
		if (running) {
			blockingTasks.add(new BlockingTask(task));
			task.afterAddedBlocking();
		}
	}

	@Nullable
	public ScheduledTask findScheduledTask(Predicate<ScheduledTask> predicate) {
		for (var task : scheduledTasks) {
			if (predicate.test(task)) {
				return task;
			}
		}

		return null;
	}

	@Nullable
	public ScheduledTask findScheduledGuildTask(Snowflake guildId, Predicate<ScheduledTask> predicate) {
		for (var task : scheduledTasks) {
			if (task.guildId.asLong() == guildId.asLong() && predicate.test(task)) {
				return task;
			}
		}

		return null;
	}

	public void restart() {
		if (!running) {
			System.exit(0);
			return;
		} else {
			running = false;
		}

		App.info("Stopping server...");
		ReactionHandler.shutdown();

		for (var task : blockingTasks) {
			task.cancelled = true;
		}

		if (webServer != null) {
			try {
				webServer.stop();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		running = false;
	}

	public void reload() {
		CharMap.load();
		GuildPaths.CUSTOM_NAMES.invalidate();

		for (var gc : db.guildCollections.values()) {
			gc.discordJS = new DiscordJS(gc, false);
		}
	}

	public void schedule(Duration timer, String type, long guild, long channel, long user, String content) {
		schedule(System.currentTimeMillis() + timer.toMillis(), type, guild, channel, user, content);
	}

	public void schedule(long end, String type, long guild, long channel, long user, String content) {
		var task = new ScheduledTask(db.scheduledTasks, type, end, guild, channel, user, content);
		db.scheduledTasks.insert(task.document.toDocument());

		if (running) {
			scheduledTasks.add(task);
		}
	}

	public void printScheduled() {
		App.info("Scheduled tasks [" + scheduledTasks.size() + "]");

		for (var task : scheduledTasks) {
			App.info(Ansi.of("- ").append(task.toAnsi()));
		}
	}
}