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
import dev.gnomebot.app.server.WSHandler;
import dev.gnomebot.app.server.WebServer;
import dev.gnomebot.app.server.handler.ActivityHandlers;
import dev.gnomebot.app.server.handler.GuildAPIHandlers;
import dev.gnomebot.app.server.handler.GuildHandlers;
import dev.gnomebot.app.server.handler.InfoHandlers;
import dev.gnomebot.app.server.handler.LogHandlers;
import dev.gnomebot.app.server.handler.MinecraftHandlers;
import dev.gnomebot.app.server.handler.PasteHandlers;
import dev.gnomebot.app.server.handler.RootHandlers;
import dev.gnomebot.app.util.BlockingTask;
import dev.gnomebot.app.util.BlockingTaskCallback;
import dev.gnomebot.app.util.CharMap;
import dev.latvian.apps.webutils.TimeUtils;
import dev.latvian.apps.webutils.ansi.Ansi;
import dev.latvian.apps.webutils.ansi.Log;
import dev.latvian.apps.webutils.ansi.Table;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;

public class App implements Runnable {
	public static final Instant START_INSTANT = Instant.now();
	public static boolean debug = false;
	public static App instance;

	public static void main(String[] args) {
		new Thread(instance = new App(), "MainThread").start();
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
		Log.INSTANCE = new GnomeLog();
		// LOGGER.replaceSystemOutLogger();
		Log.info("Starting up...");
		Locale.setDefault(Locale.US);
		System.setProperty("java.awt.headless", "true");
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");
		System.out.println("Log start: " + Instant.now());

		running = true;

		Log.info("Loading char map...");
		CharMap.load();

		cli = new CLI(this);
		cli.start();
		db = new Databases(this);

		Log.info("Loading discord handler...");

		discordHandler = new DiscordHandler(this);
		discordHandler.load();
		Log.info("Discord handler loaded");

		db.createSelfToken();

		pingHandler = new PingHandler(db);

		Log.info("Loading web server...");
		webServer = new WebServer(this);

		webServer.add("/", RootHandlers::root).noAuth();
		webServer.add("/login", RootHandlers::login).noAuth();
		webServer.add("/logout", RootHandlers::logout);
		webServer.add("/robots.txt", RootHandlers::robots).noAuth().cacheDays(1);
		webServer.add("/public/:file", RootHandlers::publicfile).noAuth().cacheMinutes(5).noLog();

		webServer.add("/api/info/ping", InfoHandlers::ping).noAuth();
		webServer.add("/api/info/user/:user", InfoHandlers::user).noAuth().cacheHours(1);
		webServer.add("/api/info/avatar/:user/:size", InfoHandlers::avatar).noAuth().cacheDays(1);
		webServer.add("/api/info/emoji/:emoji/:size", InfoHandlers::emoji).noAuth().cacheDays(1);
		webServer.add("/api/info/define/:word", InfoHandlers::define).noAuth().cacheMinutes(1);
		webServer.add("/api/info/video-thumbnail/:channel/:message/:attachment", InfoHandlers::videoThumbnail).noAuth().cacheDays(1);

		webServer.add("/api/guilds", GuildAPIHandlers::guilds).cacheMinutes(1);
		webServer.add("/api/guild/info/:guild", GuildAPIHandlers::info).member().cacheHours(1);
		webServer.add("/api/guild/banner/:guild", GuildAPIHandlers::banner).noAuth().cacheSeconds(1);
		webServer.add("/api/guild/settings/basic/:guild", GuildAPIHandlers::getSettings).member();
		webServer.add("/api/guild/settings/basic/:guild/:key", GuildAPIHandlers::updateSetting).patch().owner();
		webServer.add("/api/guild/icon/:guild/:size", GuildAPIHandlers::icon).noAuth().cacheDays(1);
		webServer.add("/paste/:id/raw", PasteHandlers::pasteRaw).noAuth().cacheMinutes(5);
		webServer.add("/paste/:id", PasteHandlers::paste).noAuth().cacheMinutes(5);
		webServer.add("/api/guild/feedback/:guild/:id", GuildAPIHandlers::feedback).member();
		webServer.add("/api/guild/feedback/:guild", GuildAPIHandlers::feedbackList).member().cacheMinutes(1);
		webServer.add("/api/guild/polls/:guild/:id", GuildAPIHandlers::poll).member();
		webServer.add("/api/guild/polls/:guild", GuildAPIHandlers::pollList).member();
		webServer.add("/api/guild/members/:guild", GuildAPIHandlers::members).member().cacheMinutes(5);
		webServer.add("/api/guild/member/:guild/:member", GuildAPIHandlers::member).member().cacheMinutes(5);
		webServer.add("/api/guild/audit-log/:guild", GuildAPIHandlers::auditLog).admin();
		webServer.add("/api/guild/export-messages/:guild/:member", GuildAPIHandlers::exportMessages).admin();

		webServer.add("/api/guild/activity/leaderboard/:guild/:days", ActivityHandlers::leaderboard).member().cacheHours(1);
		webServer.add("/api/guild/activity/leaderboard-image/:guild/:days", ActivityHandlers::leaderboardImage).member().cacheHours(1);
		webServer.add("/api/guild/activity/rank/:guild/:member/:days", ActivityHandlers::rank).member().cacheHours(0); // 1
		webServer.add("/api/guild/activity/members/:guild", ActivityHandlers::members).member().cacheMinutes(5);
		webServer.add("/api/guild/activity/channels/:guild", ActivityHandlers::channels).member().cacheMinutes(5);
		webServer.add("/api/guild/activity/user-mention-leaderboard-image/:guild/:mention/:days", ActivityHandlers::userMentionLeaderboardImage).admin().cacheHours(1);
		webServer.add("/api/guild/activity/role-mention-leaderboard-image/:guild/:mention/:days", ActivityHandlers::roleMentionLeaderboardImage).admin().cacheHours(1);

		webServer.add("/guild", GuildHandlers::guildList);
		webServer.add("/guild/:guild", GuildHandlers::guild).member();
		webServer.add("/guild/:guild/members/:id", GuildHandlers::memberInfo);
		webServer.add("/guild/:guild/audit-log", LogHandlers::auditLog).admin();
		webServer.add("/guild/:guild/macros", GuildHandlers::macros);
		webServer.add("/guild/:guild/macros/:id", GuildHandlers::macroInfo);
		webServer.add("/guild/:guild/bans", LogHandlers::bans).admin();
		webServer.add("/guild/:guild/mutes", LogHandlers::mutes).admin();
		webServer.add("/guild/:guild/feedback", GuildHandlers::feedbackList).member();
		webServer.add("/guild/:guild/feedback/:id", GuildHandlers::feedback).member();

		webServer.add("/minecraft/verify", MinecraftHandlers::verify).noAuth();

		webServer.addWS("/api/cli", WSHandler.CLI);

		webServer.start(Config.get().port);

		Log.info("");
		Log.info("API Endpoints:");
		Log.info("");

		var table = new Table("Method", "Path", "Auth Level", "Cache");

		for (var wrapper : webServer.handlerList) {
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

		Log.info("");

		ScamHandler.loadDomains();
		DM.loadDmChannels();

		try {
			ApplicationCommands.updateCommands(discordHandler);
		} catch (Exception ex) {
			Log.error("Failed to update commands:");
			ex.printStackTrace();
		}

		pingHandler.getPings();

		Log.success("Server restarted!");
		new WatchdogThread(this).start();

		try {
			while (running) {
				if (!blockingTasks.isEmpty()) {
					var task = blockingTasks.removeFirst();
					task.callback.run(task);
				}

				if (!scheduledTasks.isEmpty()) {
					var now = System.currentTimeMillis();

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
			Log.error("Server crashed!");
			ex.printStackTrace();
		}

		Log.info("Stopping the server...");
		webServer.stop();
		discordHandler.client.logout().block();
		Log.info("Server stopped!");
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
	public ScheduledTask findScheduledGuildTask(long guildId, Predicate<ScheduledTask> predicate) {
		for (var task : scheduledTasks) {
			if (task.guildId == guildId && predicate.test(task)) {
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

		Log.info("Stopping server...");
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
		GuildPaths.INVERTED_CUSTOM_NAMES.invalidate();

		for (var gc : db.guildCollections.values()) {
			gc.discordJS = new DiscordJS(gc, false);
		}
	}

	public void schedule(Duration timer, String type, long guild, long channel, long user, String content) {
		schedule(System.currentTimeMillis() + timer.toMillis(), type, guild, channel, user, content);
	}

	public void schedule(long end, String type, long guild, long channel, long user, String content) {
		var task = new ScheduledTask(db.scheduledTasksDB, type, end, guild, channel, user, content);
		db.scheduledTasksDB.insert(task.document.toDocument());

		if (running) {
			scheduledTasks.add(task);
		}
	}
}