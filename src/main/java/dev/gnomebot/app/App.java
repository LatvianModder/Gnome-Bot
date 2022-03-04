package dev.gnomebot.app;

import dev.gnomebot.app.data.Databases;
import dev.gnomebot.app.data.GuildCollections;
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
import dev.gnomebot.app.server.handler.MiscHandlers;
import dev.gnomebot.app.server.handler.PanelHandlers;
import dev.gnomebot.app.server.handler.PasteHandlers;
import dev.gnomebot.app.server.handler.SpecialHandlers;
import dev.gnomebot.app.util.Ansi;
import dev.gnomebot.app.util.BlockingTask;
import dev.gnomebot.app.util.BlockingTaskCallback;
import dev.gnomebot.app.util.CharMap;
import dev.gnomebot.app.util.ScheduledTask;
import dev.gnomebot.app.util.ScheduledTaskCallback;
import dev.gnomebot.app.util.Table;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.presence.ClientPresence;
import discord4j.rest.http.client.ClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class App implements Runnable {
	public static final Instant START_INSTANT = Instant.now();
	public static final Logger LOGGER = new Logger();
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
	public ArrayList<BlockingTask> blockingTasks = new ArrayList<>();
	public final Object blockingTaskLock = new Object();
	public ArrayList<ScheduledTask> scheduledTasks = new ArrayList<>();
	public final Object scheduledTaskLock = new Object();

	public Databases db;
	public WebServer webServer;
	public DiscordHandler discordHandler;
	// TODO: Move more handlers from static instances to here
	public PingHandler pingHandler;

	@Override
	public void run() {
		Locale.setDefault(Locale.US);
		System.setProperty("java.awt.headless", "true");
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");

		running = true;
		blockingTasks = new ArrayList<>();

		CharMap.load();

		ConsoleCommandManager commands = new ConsoleCommandManager(this);

		commands.add("restart", matcher -> restart());
		commands.add("reload", matcher -> reload());
		commands.add("token", matcher -> info(Utils.createToken()));
		commands.add(Pattern.compile("^leave_guild (\\d+)$"), matcher -> leaveGuild(Snowflake.of(matcher.group(1))));

		commands.add(Pattern.compile("^remove_modifiers (.*)$"), matcher -> info(CharMap.MODIFIER_PATTERN.matcher(matcher.group(1)).replaceAll("")));

		commands.add("colors", matcher -> {
			StringBuilder line1 = new StringBuilder();
			StringBuilder line2 = new StringBuilder();

			for (int i = 0; i < 8; i++) {
				line1.append("\u001B[0;3").append(i).append("m■ ");
				line2.append("\u001B[1;3").append(i).append("m■ ");
			}

			info(line1);
			info(line2);
		});

		commands.add(Pattern.compile("^echo_cli (.*)$"), matcher -> {
			String message = matcher.group(1);
			info("Sending to all CLI clients: " + message);
			WSHandler.CLI.broadcast(message);
		});

		db = new Databases(this);

		commands.startThread();

		discordHandler = new DiscordHandler(this);
		discordHandler.load();
		App.info("Discord handler loaded");

		db.createSelfToken();

		pingHandler = new PingHandler(db);

		webServer = new WebServer(this);

		webServer.add("assets/:filename", MiscHandlers::assets).noAuth().cacheHours(1);
		webServer.add("api/account/sign-in", MiscHandlers::signIn).noAuth().log();
		webServer.add("api/account/sign-out", MiscHandlers::signOut).post().log();

		webServer.add("api/info/ping", InfoHandlers::ping).noAuth();
		webServer.add("api/info/user/:user", InfoHandlers::user).noAuth().cacheHours(1);
		webServer.add("api/info/avatar/:user/:size", InfoHandlers::avatar).noAuth().cacheDays(1);
		webServer.add("api/info/emoji/:emoji/:size", InfoHandlers::emoji).noAuth().cacheDays(1);
		webServer.add("api/info/define/:word", InfoHandlers::define).noAuth().cacheMinutes(1);

		webServer.add("api/guilds", GuildHandlers::guilds).cacheMinutes(1).log();
		webServer.add("api/guild/info/:guild", GuildHandlers::info).member().cacheHours(1);
		webServer.add("api/guild/banner/:guild", GuildHandlers::banner).noAuth().cacheSeconds(1);
		webServer.add("api/guild/settings/basic/:guild", GuildHandlers::getSettings).member();
		webServer.add("api/guild/settings/basic/:guild/:setting", GuildHandlers::updateSetting).patch().owner();
		webServer.add("api/guild/icon/:guild/:size", GuildHandlers::icon).noAuth().cacheDays(1);
		webServer.add("api/guild/file/:channel/:id/:filename", PasteHandlers::file).noAuth().cacheMinutes(5);
		webServer.add("paste/:id/raw", PasteHandlers::pasteRaw).noAuth().cacheMinutes(5);
		webServer.add("paste/:id", PasteHandlers::paste).noAuth().cacheMinutes(5);
		webServer.add("api/guild/feedback/:guild/:id", GuildHandlers::feedback).member();
		webServer.add("api/guild/feedback/:guild", GuildHandlers::feedbackList).member().cacheMinutes(1);
		webServer.add("api/guild/polls/:guild/:id", GuildHandlers::poll).member();
		webServer.add("api/guild/polls/:guild", GuildHandlers::pollList).member();
		webServer.add("api/guild/members/:guild", GuildHandlers::members).member().cacheMinutes(5);
		webServer.add("api/guild/member/:guild/:member", GuildHandlers::member).member().cacheMinutes(5);
		webServer.add("api/guild/unpingable-names/:guild", GuildHandlers::unpingableNames).admin();
		webServer.add("api/guild/audit-log/:guild", GuildHandlers::auditLog).admin().log();
		webServer.add("api/guild/slur-regex/:guild", GuildHandlers::slurRegex).member().log();
		webServer.add("api/guild/export-messages/:guild/:member", GuildHandlers::exportMessages).admin().log();

		webServer.add("api/guild/activity/leaderboard/:guild/:days", ActivityHandlers::leaderboard).member().cacheHours(1);
		webServer.add("api/guild/activity/leaderboard-image/:guild/:days", ActivityHandlers::leaderboardImage).member().cacheHours(1);
		webServer.add("api/guild/activity/members/:guild", ActivityHandlers::members).member().cacheMinutes(5);
		webServer.add("api/guild/activity/channels/:guild", ActivityHandlers::channels).member().cacheMinutes(5);

		webServer.add("public/:file", SpecialHandlers::publicfile).noAuth().cacheMinutes(5);

		webServer.add("panel", PanelHandlers::root).log();
		webServer.add("panel/login", PanelHandlers::login).noAuth().log();
		webServer.add("panel/:guild", PanelHandlers::guild).member();
		webServer.add("panel/:guild/offenses", PanelHandlers::offenses).member();
		webServer.add("panel/:guild/offenses/:user", PanelHandlers::offensesOf).admin();
		webServer.add("panel/:guild/audit-log", PanelHandlers::auditLog).admin();

		webServer.addWS("api/cli", WSHandler.CLI);

		webServer.start(Config.get().port);

		info("");
		info("API Endpoints:");
		info("");

		Table table = new Table("Method", "Path", "Auth Level", "Cache");

		for (RequestHandler wrapper : webServer.handlerList) {
			Table.Cell[] cells = table.addRow();

			switch (wrapper.method) {
				case GET -> cells[0].value(Ansi.GREEN + "GET");
				case POST -> cells[0].value(Ansi.CYAN + "POST");
				case PATCH -> cells[0].value(Ansi.TEAL + "PATCH");
				case DELETE -> cells[0].value(Ansi.RED + "DELETE");
				case PUT -> cells[0].value(Ansi.YELLOW + "PUT");
			}

			StringBuilder psb = new StringBuilder();

			for (String p : wrapper.path.split("/")) {
				psb.append(Ansi.CYAN + '/');

				if (p.startsWith(":")) {
					psb.append(Ansi.YELLOW);
					psb.append(p.substring(1));
				} else {
					psb.append(p);
				}
			}

			cells[1].value(psb);
			cells[2].value((wrapper.authLevel == AuthLevel.NO_AUTH ? Ansi.YELLOW : Ansi.GREEN) + wrapper.authLevel.name().toLowerCase());

			if (wrapper.cacheSeconds == 0) {
				cells[3].value(Ansi.YELLOW + "no-cache");
			} else {
				cells[3].value(Ansi.GREEN + (wrapper.authLevel == AuthLevel.NO_AUTH ? "public " : "private ") + Utils.prettyTimeString(wrapper.cacheSeconds));
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

		success("Server restarted!");
		// queueScheduledTask(System.currentTimeMillis() + 3000L, task -> info("Test 1!"));
		// queueScheduledTask(System.currentTimeMillis() + 4000L, task -> info("Test 2!"));
		// queueScheduledTask(System.currentTimeMillis() + 5000L, task -> info("Test 3!"));

		new WatchdogThread(this).start();

		try {
			while (running) {
				synchronized (blockingTaskLock) {
					if (!blockingTasks.isEmpty()) {
						BlockingTask task = blockingTasks.get(0);
						task.callback.run(task);
						blockingTasks.remove(0);
					}
				}

				synchronized (scheduledTaskLock) {
					if (!scheduledTasks.isEmpty()) {
						long now = System.currentTimeMillis();

						Iterator<ScheduledTask> itr = scheduledTasks.iterator();

						while (itr.hasNext()) {
							ScheduledTask t = itr.next();

							if (now >= t.end) {
								t.callback.run(t);
								itr.remove();
							}
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
			synchronized (blockingTaskLock) {
				blockingTasks.add(new BlockingTask(task));
				task.afterAddedBlocking();
			}
		}
	}

	public void queueScheduledTask(long end, ScheduledTaskCallback task) {
		if (running) {
			synchronized (scheduledTaskLock) {
				scheduledTasks.add(new ScheduledTask(task, end));
				task.afterAddedScheduled();
			}
		}
	}

	public void restart() {
		if (!running) {
			System.exit(0);
			return;
		}

		try {
			App.instance.discordHandler.client.updatePresence(ClientPresence.invisible()).timeout(Duration.ofMillis(3000L)).block();
		} catch (Exception ex) {
		}

		ReactionHandler.shutdown();

		for (BlockingTask task : blockingTasks) {
			task.cancelled = true;
		}

		running = false;
	}

	public void reload() {
		CharMap.load();

		for (GuildCollections gc : db.guildCollections.values()) {
			gc.discordJS = new DiscordJS(gc, false);
		}
	}

	public void leaveGuild(Snowflake id) {
		discordHandler.client.getGuildById(id).flatMap(Guild::leave).subscribe();
	}
}