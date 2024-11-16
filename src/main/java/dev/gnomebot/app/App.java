package dev.gnomebot.app;

import dev.gnomebot.app.data.Databases;
import dev.gnomebot.app.data.RelatedGuild;
import dev.gnomebot.app.data.ScheduledTask;
import dev.gnomebot.app.data.ping.PingHandler;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.DiscordHandler;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.ReactionHandler;
import dev.gnomebot.app.discord.ScamHandler;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.script.DiscordJS;
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
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.ansi.ANSI;
import dev.latvian.apps.ansi.command.CLI;
import dev.latvian.apps.ansi.command.CommandManager;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.ansi.terminal.Terminal;
import dev.latvian.apps.json.JSON;
import dev.latvian.apps.json.JSONArray;
import dev.latvian.apps.webutils.FormattingUtils;
import dev.latvian.apps.webutils.ansi.Table;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class App {
	public static final Instant START_INSTANT = Instant.now();
	public static boolean debug = false;
	public static App instance;

	public static void main(String[] args) {
		new App();
	}

	public static String url(String path) {
		if (path.isEmpty()) {
			return Config.get().panel_url;
		}

		return Config.get().panel_url + path;
	}

	public boolean running;
	public ConcurrentLinkedDeque<BlockingTask> blockingTasks = new ConcurrentLinkedDeque<>();
	public ConcurrentLinkedDeque<ScheduledTask> scheduledTasks = new ConcurrentLinkedDeque<>();

	public Databases db;
	public WebServer webServer;
	public DiscordHandler discordHandler;
	// TODO: Move more handlers from static instances to here
	public PingHandler pingHandler;

	public App() {
		instance = this;
		Terminal.replaceSystemLog();

		// LOGGER.replaceSystemOutLogger();
		Log.info("Starting up...");
		Locale.setDefault(Locale.US);
		System.setProperty("java.awt.headless", "true");
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");
		System.out.println("Log start: " + Instant.now());

		running = true;

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
		webServer.add("/paste/mclogs/:id", PasteHandlers::pasteMclogs).noAuth().cacheMinutes(5);
		webServer.add("/paste/:channel/:message/:id/raw", PasteHandlers::pasteRaw).noAuth().cacheMinutes(5);
		webServer.add("/paste/:channel/:message/:id", PasteHandlers::paste).noAuth().cacheMinutes(5);
		webServer.add("/new-paste/:channel/:message/:id", PasteHandlers::newPaste).noAuth();
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
		webServer.add("/guild/:guild/appeal", GuildHandlers::appeal).noAuth();

		webServer.add("/minecraft/verify", MinecraftHandlers::verify).noAuth();

		// fail();

		// webServer.addWS("/api/cli", WSHandler.CLI);

		webServer.start(Config.get().port);

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

	private void rebuild() throws Exception {
		Log.error(ANSI.of("Building...").white().orangeBG());

		var process = new ProcessBuilder("./gradlew", "shadowJar").start();
		int result = process.waitFor();

		if (result == 0) {
			Log.info(ANSI.of("Building finished with no errors!").white().greenBG());
			restart();
		} else {
			Log.error("Building finished with error code " + result);
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

	private void port(Map<String, String> args) throws Exception {
		var mm = db.guild(Snowflake.of(166630061217153024L));
		var count = 0L;

		/*
		var invite = app.discordHandler.client.getRestClient().getInviteService().getInvite(input).block();
		// 6v3z26B

		Log.info("Invite Data: " + invite);

		var guild = invite.guild().toOptional().orElse(null);
		Log.info("Invite Guild Members: " + invite.approximatePresenceCount().toOptional().orElse(0) + " / " + invite.approximateMemberCount().toOptional().orElse(0));

		if (guild != null) {
			Log.info("Invite Guild: " + guild.name());
			Log.info("- Icon: " + guild.icon().orElse(""));
			Log.info("- Description: " + guild.description().orElse(""));
			Log.info("- Vanity URL: " + guild.vanityUrlCode().orElse(""));
			Log.info("- Members: " + guild.approximatePresenceCount().toOptional().orElse(0) + " / " + guild.approximateMemberCount().toOptional().orElse(0));
		}
		 */

		var group = new RelatedGuild.Group(0, "Mods", "", new ArrayList<>());
		var pattern = Pattern.compile("^(.+?)(?: \\((.+)\\))?: (\\w+)$");

		for (var line : Files.readAllLines(mm.paths.path.resolve("related_guilds.txt"))) {
			line = line.trim();

			if (line.isEmpty()) {
				continue;
			}

			var matcher = pattern.matcher(line);

			if (matcher.find()) {
				count++;
				var name = matcher.group(1);
				var description = Optional.ofNullable(matcher.group(2)).orElse("");
				var invite = matcher.group(3);
				var icon = "";
				var guildId = 0L;

				try {
					var inv = discordHandler.client.getRestClient().getInviteService().getInvite(invite).block();
					var guild = inv.guild().toOptional().orElse(null);

					if (guild != null) {
						guildId = guild.id().asLong();
						name = guild.name();
						icon = guild.icon().orElse("");

						if (description.isEmpty()) {
							description = guild.description().orElse("");
						}

						invite = guild.vanityUrlCode().orElse(invite);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				Log.info(name + " (" + description + "): " + invite + " (" + icon + ")");

				group.guilds().add(new RelatedGuild(group, group.guilds().size(), invite, guildId, name, description, icon));
			}
		}

		Collections.reverse(group.guilds());
		var json = JSONArray.of();
		json.add(group.toJson());
		JSON.DEFAULT.write(mm.paths.path.resolve("related_guilds.json"), json, false);

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

		Log.success("Done %,d".formatted(count));
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
		var table = new Table("Name", "Owner", "Members", "Messages", "Gnome Messages", "ID");

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
				.addComponent(ActionRow.of(discord4j.core.object.component.Button.danger("restart-bot/" + Config.get().restart_button_token, Emojis.ALERT, "Restart")))
				.build()
		).subscribe();
	}
}