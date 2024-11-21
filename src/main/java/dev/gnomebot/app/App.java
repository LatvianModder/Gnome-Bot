package dev.gnomebot.app;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import dev.gnomebot.app.data.Databases;
import dev.gnomebot.app.data.ScheduledTask;
import dev.gnomebot.app.data.ping.PingHandler;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.DiscordHandler;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.ReactionHandler;
import dev.gnomebot.app.discord.ScamHandler;
import dev.gnomebot.app.discord.WebHookDestination;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.script.DiscordJS;
import dev.gnomebot.app.server.AppRequest;
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
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.ansi.ANSI;
import dev.latvian.apps.ansi.ANSITable;
import dev.latvian.apps.ansi.command.CLI;
import dev.latvian.apps.ansi.command.CommandManager;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.ansi.terminal.Terminal;
import dev.latvian.apps.json.JSON;
import dev.latvian.apps.tinyserver.HTTPServer;
import dev.latvian.apps.tinyserver.OptionalString;
import dev.latvian.apps.tinyserver.http.file.FileResponseHandler;
import dev.latvian.apps.webutils.FormattingUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public class App {
	public static final Instant START_INSTANT = Instant.now();
	public static boolean debug = false;
	public static App instance;

	public static void main(String[] args) throws IOException {
		new App();
	}

	public boolean running;
	public final Config config;
	public ConcurrentLinkedDeque<BlockingTask> blockingTasks = new ConcurrentLinkedDeque<>();
	public ConcurrentLinkedDeque<ScheduledTask> scheduledTasks = new ConcurrentLinkedDeque<>();

	public Databases db;
	public HTTPServer<AppRequest> webServer;
	public DiscordHandler discordHandler;
	// TODO: Move more handlers from static instances to here
	public PingHandler pingHandler;

	public App() throws IOException {
		instance = this;
		Terminal.replaceSystemLog();

		// LOGGER.replaceSystemOutLogger();
		Log.info("Starting up...");
		Locale.setDefault(Locale.US);
		System.setProperty("java.awt.headless", "true");
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");
		System.out.println("Log start: " + Instant.now());

		this.running = true;

		JSON.DEFAULT.registerAdapter(WebHookDestination.class, (json, o, type) -> o == null ? null : new WebHookDestination(o.toString()));
		this.config = JSON.DEFAULT.read(AppPaths.CONFIG_FILE).adapt(Config.class);

		Log.info("Loading char map...");
		CharMap.load();

		var commands = new CommandManager();
		commands.add("restart", this::restart);
		commands.add("rebuild", this::rebuild);
		commands.add("reload", this::reload);
		commands.add("port", List.of("input"), this::port);
		commands.add("stats", this::stats);
		commands.add("debug", this::debug);
		commands.add("token", () -> Log.info(Utils.createToken()));
		commands.add("short_token", () -> Log.info(Utils.createShortToken()));
		commands.add("leave_guild", List.of("guild"), args -> leaveGuild(args.get("guild")));
		commands.add("remove_modifiers", List.of("text"), args -> Log.info(CharMap.MODIFIER_PATTERN.matcher(args.get("text")).replaceAll("")));
		commands.add("echo_cli", List.of("message..."), args -> echoCli(args.get("message")));
		commands.add("guilds", this::printGuilds);
		commands.add("create_restart_button", List.of("channel"), args -> createRestartButton(args.get("channel")));
		CLI.start(commands);

		db = new Databases(this);

		Log.info("Loading discord handler...");

		discordHandler = new DiscordHandler(this);
		discordHandler.load();
		Log.info("Discord handler loaded");

		db.createSelfToken();

		pingHandler = new PingHandler(db);

		Log.info("Loading web server...");
		webServer = new HTTPServer<>(this::createAppRequest);

		webServer.get("/", RootHandlers::root);
		webServer.get("/login", RootHandlers::login);
		webServer.get("/logout", RootHandlers::logout);
		webServer.get("/robots.txt", RootHandlers::robots);
		webServer.get("/public/{file}", RootHandlers::publicfile);

		webServer.get("/api/info/ping", InfoHandlers::ping);
		webServer.get("/api/info/user/{user}", InfoHandlers::user);
		webServer.get("/api/info/avatar/{user}/{size}", InfoHandlers::avatar);
		webServer.get("/api/info/emoji/{emoji}/{size}", InfoHandlers::emoji);
		webServer.get("/api/info/define/{word}", InfoHandlers::define);
		webServer.get("/api/info/video-thumbnail/{channel}/{message}/{attachment}", InfoHandlers::videoThumbnail);

		webServer.get("/api/guilds", GuildAPIHandlers::guilds);
		webServer.get("/api/guild/info/{guild}", GuildAPIHandlers::info);
		webServer.get("/api/guild/banner/{guild}", GuildAPIHandlers::banner);
		webServer.get("/api/guild/settings/basic/{guild}", GuildAPIHandlers::getSettings);
		webServer.patch("/api/guild/settings/basic/{guild}/{key}", GuildAPIHandlers::updateSetting);
		webServer.get("/api/guild/icon/{guild}/{size}", GuildAPIHandlers::icon);
		webServer.get("/paste/mclogs/{id}", PasteHandlers::pasteMclogs);
		webServer.get("/paste/{channel}/{message}/{id}/raw", PasteHandlers::pasteRaw);
		webServer.get("/paste/{channel}/{message}/{id}", PasteHandlers::paste);
		webServer.get("/new-paste/{channel}/{message}/{id}", PasteHandlers::newPaste);
		webServer.get("/api/guild/feedback/{guild}/{id}", GuildAPIHandlers::feedback);
		webServer.get("/api/guild/feedback/{guild}", GuildAPIHandlers::feedbackList);
		webServer.get("/api/guild/polls/{guild}", GuildAPIHandlers::pollList);
		webServer.get("/api/guild/polls/{guild}/{id}", GuildAPIHandlers::poll);
		webServer.get("/api/guild/members/{guild}", GuildAPIHandlers::members);
		webServer.get("/api/guild/member/{guild}/{member}", GuildAPIHandlers::member);
		webServer.get("/api/guild/audit-log/{guild}", GuildAPIHandlers::auditLog);
		webServer.get("/api/guild/export-messages/{guild}/{member}", GuildAPIHandlers::exportMessages);

		webServer.get("/api/guild/activity/leaderboard/{guild}/{days}", ActivityHandlers::leaderboard);
		webServer.get("/api/guild/activity/leaderboard-image/{guild}/{days}", ActivityHandlers::leaderboardImage);
		webServer.get("/api/guild/activity/rank/{guild}/{member}/{days}", ActivityHandlers::rank);
		webServer.get("/api/guild/activity/members/{guild}", ActivityHandlers::members);
		webServer.get("/api/guild/activity/channels/{guild}", ActivityHandlers::channels);
		webServer.get("/api/guild/activity/user-mention-leaderboard-image/{guild}/{mention}/{days}", ActivityHandlers::userMentionLeaderboardImage);
		webServer.get("/api/guild/activity/role-mention-leaderboard-image/{guild}/{mention}/{days}", ActivityHandlers::roleMentionLeaderboardImage);

		webServer.get("/guild", GuildHandlers::guildList);
		webServer.get("/guild/{guild}", GuildHandlers::guild);
		webServer.get("/guild/{guild}/members/{id}", GuildHandlers::memberInfo);
		webServer.get("/guild/{guild}/audit-log", LogHandlers::auditLog);
		webServer.get("/guild/{guild}/macros", GuildHandlers::macros);
		webServer.get("/guild/{guild}/macros/{id}", GuildHandlers::macroInfo);
		webServer.get("/guild/{guild}/bans", LogHandlers::bans);
		webServer.get("/guild/{guild}/mutes", LogHandlers::mutes);
		webServer.get("/guild/{guild}/feedback", GuildHandlers::feedbackList);
		webServer.get("/guild/{guild}/feedback/{id}", GuildHandlers::feedback);
		webServer.get("/guild/{guild}/appeal", GuildHandlers::appeal);

		webServer.get("/minecraft/verify", MinecraftHandlers::verify);

		webServer.staticFiles("/", AppPaths.RESOURCES, FileResponseHandler.CACHE_1_HOUR, true);

		// webServer.addWS("/api/cli", WSHandler.CLI);

		webServer.setPort(config.web.port);
		webServer.setKeepAliveTimeout(Duration.ofMinutes(5L));
		webServer.start();

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

	private AppRequest createAppRequest() {
		return new AppRequest(this);
	}

	public String url(String path) {
		if (path.isEmpty()) {
			return config.web.panel_url + '/';
		}

		return config.web.panel_url + '/' + path;
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

	private void rebuild() throws Exception {
		Log.error(ANSI.of("Building...").white().orangeBG());

		var process = new ProcessBuilder("./gradlew", "shadowJar").start();
		int result = process.waitFor();

		if (result == 0) {
			Log.info(ANSI.of("Building finished with no errors!").white().greenBG());
			restart();
		} else {
			Log.error("Building finished with error code " + result);

			try (var err = process.getErrorStream()) {
				Log.error(new String(err.readAllBytes()));
			}
		}
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

	private void port(Map<String, String> args) {
		var count = new AtomicLong(0L);
		var max = new AtomicLong(0L);
		var done = new AtomicBoolean(false);
		long now = System.currentTimeMillis();

		var countThread = new Thread(() -> {
			try {
				while (!done.get()) {
					Thread.sleep(5000L);
					Log.warn("%,d / %,d".formatted(count.get(), max.get()));
				}

				Log.success("Done %,d / %,d in %.02f s".formatted(count.get(), max.get(), (System.currentTimeMillis() - now) / 1000F));
			} catch (Exception ignored) {
			}
		});

		countThread.setDaemon(true);
		countThread.start();

		var thread = new Thread(() -> {
			try {
				var args1 = new LinkedHashMap<String, OptionalString>();

				for (var entry : args.entrySet()) {
					args1.put(entry.getKey(), OptionalString.of(entry.getValue()));
				}

				port(args1, now, count, max);
			} catch (Throwable ex) {
				ex.printStackTrace();
			}

			done.set(true);
		});

		thread.setDaemon(true);
		thread.start();
	}

	private void port(Map<String, OptionalString> args, long now, AtomicLong count, AtomicLong max) throws Throwable {
		var mm = db.guild(Snowflake.of(166630061217153024L));

		// DELETE
		for (var gc : db.allGuilds()) {
			Log.info("Porting " + gc);
			// gc.auditLog.query().filter(Filters.or(Filters.eq("type", "command"))).many().delete();

			gc.reactionLog.query().filter(Filters.or(Filters.eq("type", "reaction_added"))).many().update(Updates.set("type", 0));
			gc.reactionLog.query().filter(Filters.or(Filters.eq("type", "reaction_removed"))).many().update(Updates.set("type", 1));
			gc.voiceLog.query().filter(Filters.or(Filters.eq("type", "join_voice"))).many().update(Updates.set("type", 0));
			gc.voiceLog.query().filter(Filters.or(Filters.eq("type", "leave_voice"))).many().update(Updates.set("type", 1));
		}

		/*
		// MOVE
		for (var gc : db.allGuilds()) {
			Log.info("Porting " + gc);

			var q = gc.auditLog.query().filter(Filters.or(Filters.eq("type", "command")));
			max.set(q.count());
			count.set(0L);

			var docs = new ArrayList<Document>();

			for (var doc0 : q.findIterable()) {
				long user = doc0.getLong("user");

				var doc = new Document();
				doc.put("_id", doc0.get("_id"));
				doc.put("user", user);
				doc.put("channel", doc0.get("channel"));
				doc.put("message", doc0.get("message"));
				doc.put("command", doc0.get("old_content", doc0.get("content")));
				doc.put("full_command", doc0.get("content"));
				docs.add(doc);
				count.addAndGet(1L);
			}

			gc.commandLog.drop();
			gc.commandLog.insertMany(docs);
		}
		 */

		/*
		var toDelete = new ArrayList<Webhook>();

		for (var gc : app.db.allGuilds()) {
			Log.warn("Done %,d".formatted(count));
			Log.info("Porting " + gc);

			for (var channel : gc.getGuild().getChannels().toIterable()) {
				if (channel instanceof TopLevelGuildMessageChannel tlc) {
					try {
						for (var w : tlc.getWebhooks().toIterable()) {
							boolean success = false;

							if (w.getToken().isPresent() && w.getCreator().map(u -> u.getId().asLong() == gc.db.app.discordHandler.selfId).orElse(false)) {
								count++;
								success = true;
								toDelete.add(w);
							}

							Log.success("- " + w.getName().orElse("Unnamed") + " by " + w.getCreator().map(User::getUsername).orElse("Unknown") + " " + w.getId().asString(), success);
						}
					} catch (Exception ex) {
						Log.warn(ex);
					}
				}
			}
		}

		for (var w : toDelete) {
			try {
				w.delete().block();
				Log.success("Deleted " + w.getId().asString());
			} catch (Exception ex) {
				Log.warn(ex);
			}
		}
		 */
	}

	private void stats() {
		Log.info("***");

		Log.info("DB Stats:");

		/*
		for (var db : app.db.collections.values()) {
			Ansi.log("- " + db.name + ":");
			Ansi.log("    Size: " + db.count);
			Ansi.log("    Queue: " + db.queue.size());
		}
		 */

		Log.info("***");
	}

	private void debug() {
		App.debug = !App.debug;
		Log.info("+ Debug mode: " + (App.debug ? "enabled" : "disabled"));
	}

	private void leaveGuild(String id) {
		discordHandler.client.getGuildById(SnowFlake.convert(id)).flatMap(Guild::leave).subscribe();
	}

	private void printGuilds() {
		var table = new ANSITable("Name", "Owner", "Members", "Messages", "Gnome Messages", "ID");

		for (var g : discordHandler.getSelfGuilds()) {
			Log.info("Loading guild " + g.getId().asString() + " " + g.getName() + "...");
			var gc = db.guild(g.getId());

			table.addRow(FormattingUtils.trim(g.getName(), 70), gc.getMember(gc.ownerId).getDisplayName(), g.getMembers().count().block(), gc.messages.count(), gc.messages.query().eq("user", discordHandler.selfId).count(), g.getId().asString());
		}

		table.print();
	}

	private void echoCli(String message) {
		Log.info("Sending to all CLI clients: " + message);
		// FIXME: WSHandler.CLI.broadcast(message);
	}

	private void createRestartButton(String s) {
		var channel = (MessageChannel) discordHandler.client.getChannelById(SnowFlake.convert(s)).block();

		channel.createMessage(MessageCreateSpec.builder()
				.content("# Restart Gnome Bot")
				.addComponent(ActionRow.of(discord4j.core.object.component.Button.danger("restart-bot/" + config.discord.restart_button_token, Emojis.ALERT, "Restart")))
				.build()
		).subscribe();
	}
}