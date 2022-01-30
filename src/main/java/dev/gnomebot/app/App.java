package dev.gnomebot.app;

import com.mongodb.client.model.Filters;
import com.sun.management.HotSpotDiagnosticMXBean;
import dev.gnomebot.app.data.Databases;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.ExportedMessage;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.WebToken;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.DiscordHandler;
import dev.gnomebot.app.discord.ReactionHandler;
import dev.gnomebot.app.discord.ScamHandler;
import dev.gnomebot.app.discord.UserCache;
import dev.gnomebot.app.script.DiscordJS;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.server.CLISession;
import dev.gnomebot.app.server.RequestHandler;
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
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.ScheduledTask;
import dev.gnomebot.app.util.ScheduledTaskCallback;
import dev.gnomebot.app.util.Table;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.presence.ClientPresence;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.rest.http.client.ClientException;
import io.javalin.websocket.WsConnectContext;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Nullable;

import javax.management.MBeanServer;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
		commands.add(Pattern.compile("^update_slash_command ([\\w-\\s]+)$"), matcher -> discordHandler.updateGlobalCommand(matcher.group(1)));
		commands.add(Pattern.compile("^delete_global_slash_command ([\\w-\\s]+)$"), matcher -> deleteGlobalSlashCommand(matcher.group(1)));
		commands.add(Pattern.compile("^delete_guild_slash_command ([\\w-\\s]+) (\\d+)$"), matcher -> deleteGuildSlashCommand(matcher.group(1), Snowflake.of(matcher.group(2))));
		commands.add("list_global_slash_commands", matcher -> listGlobalSlashCommands());
		commands.add(Pattern.compile("^list_guild_slash_commands (\\d+)$"), matcher -> listGuildSlashCommands(Snowflake.of(matcher.group(1))));
		commands.add(Pattern.compile("^leave_guild (\\d+)$"), matcher -> leaveGuild(Snowflake.of(matcher.group(1))));
		commands.add("heapdump", matcher -> heapdump());
		commands.add("dbs", matcher -> printDatabases());
		commands.add("fonts", matcher -> printFonts());

		commands.add(Pattern.compile("^remove_modifiers (.*)$"), matcher -> info(CharMap.MODIFIER_PATTERN.matcher(matcher.group(1)).replaceAll("")));

		commands.add(Pattern.compile("^regex_find (.*)$"), matcher -> {
			Pattern pattern = Pattern.compile(matcher.group(1), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
			UserCache cache = discordHandler.createUserCache();

			for (DiscordMessage m : db.guildModdedMC().messages.query().regex("content", pattern).limit(100).descending("timestamp")) {
				info(Ansi.CYAN + m.getUIDSnowflake().asString() + " / " + m.getDate().toInstant() + Ansi.GREEN + " #" + "unknown" + " " + Ansi.YELLOW + cache.get(Snowflake.of(m.getUserID())).get().getUsername() + ": " + Ansi.RESET + m.getContent());
			}
		});

		commands.add(Pattern.compile("^isolate_convo (.+)$"), matcher -> {
			List<Bson> userList = new ArrayList<>();

			for (String s : matcher.group(1).split(" ")) {
				userList.add(Filters.eq("user", db.guildModdedMC().getUserID(s)));
			}

			printMessageTable(Collections.singletonList(Filters.or(userList)), 200);
		});

		commands.add(Pattern.compile("^print_dms (.+)$"), matcher -> {
			PrivateChannel channel;
			try {
				channel = DM.open(discordHandler.getUser(Snowflake.of(matcher.group(1))));
				Snowflake lastId = channel.getLastMessageId().orElse(null);

				if (lastId == null) {
					error("No DMs!");
					return;
				}

				Table table = new Table("From", "Timestamp", "Title", "Content");

				Message message = channel.getMessageById(lastId).block();

				if (!message.getContent().isEmpty()) {
					table.addRow(message.getUserData().id().asString(), message.getTimestamp().toString(), "", message.getContent());
				}

				for (Embed embed : message.getEmbeds()) {
					table.addRow(message.getUserData().id().asString(), message.getTimestamp().toString(), embed.getTitle(), embed.getDescription());
				}

				for (Message message1 : channel.getMessagesBefore(lastId).toIterable()) {
					if (!message1.getContent().isEmpty()) {
						table.addRow(message1.getUserData().id().asString(), message1.getTimestamp().toString(), "", message1.getContent());
					}

					for (Embed embed : message1.getEmbeds()) {
						table.addRow(message1.getUserData().id().asString(), message1.getTimestamp().toString(), embed.getTitle(), embed.getDescription());
					}
				}

				table.print();
				Files.write(AppPaths.DATA_GUILDS.resolve("dms-" + matcher.group(1) + ".txt"), table.getCSVBytes(false));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		commands.add("last_deleted_messages", matcher -> {
			//printMessageTable(Collections.singletonList(Filters.bitsAnySet("flags", DiscordMessage.FLAG_DELETED)), 50);
		});

		commands.add(Pattern.compile("^guilds( \\d+)?$"), matcher -> printGuilds(matcher.group(1)));

		commands.add(Pattern.compile("^find_bad_words (.+)$"), matcher -> {
			printMessageTable(Arrays.asList(Filters.eq("user", db.guildModdedMC().getUserID(matcher.group(1))), Filters.regex("content", db.guildModdedMC().badWordRegex)), 1000);
		});

		commands.add(Pattern.compile("^export_messages (.+)$"), matcher -> {
			long id = db.guildModdedMC().getUserID(matcher.group(1));
			LinkedList<ExportedMessage> list = new LinkedList<>();

			for (DiscordMessage m : db.guildModdedMC().messages.query().eq("user", id)) {
				if (list.size() % 10000 == 0) {
					info("Gathered " + list.size() + " so far...");
				}

				ExportedMessage message = new ExportedMessage();
				message.timestamp = m.getDate().getTime();
				message.channel = m.getChannelID();
				message.flags = m.flags;
				message.content = m.getContent();
				list.add(message);
			}

			info("Done gathering messages! Sorting...");

			list.sort(ExportedMessage.COMPARATOR);

			info("Done sorting! Saving to file...");

			CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
			try (OutputStream out = Files.newOutputStream(AppPaths.DATA_GUILDS.resolve(id + ".csv"));
				 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, encoder))) {
				for (ExportedMessage line : list) {
					writer.append(line.toString());
					writer.newLine();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			info("Done!");
		});

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
			CLISession.Handler.INSTANCE.broadcast(message);
		});

		db = new Databases(this);

		commands.startThread();

		discordHandler = new DiscordHandler(this);
		discordHandler.load();
		App.info("Discord handler loaded");

		Document tokenDoc = new Document();
		tokenDoc.put("_id", Config.get().self_token);
		tokenDoc.put("created", new Date());
		tokenDoc.put("user", discordHandler.selfId.asLong());
		tokenDoc.put("name", "GnomeBot");
		tokenDoc.put("user_agent", "localhost");
		Utils.selfToken = new WebToken(db.webTokens, MapWrapper.wrap(tokenDoc));

		webServer = new WebServer(this);

		webServer.add("assets/:filename", MiscHandlers::assets).noAuth().cacheHours(1).desc("Assets");
		webServer.add("api/account/sign-in", MiscHandlers::signIn).noAuth().log().desc("Sign in");
		webServer.add("api/account/sign-out", MiscHandlers::signOut).post().log().desc("Sign out");

		webServer.add("api/info/ping", InfoHandlers::ping).noAuth().desc("Ping");
		webServer.add("api/info/user/:user", InfoHandlers::user).noAuth().cacheHours(1).desc("User info");
		webServer.add("api/info/avatar/:user/:size", InfoHandlers::avatar).noAuth().cacheDays(1).desc("User avatar");
		webServer.add("api/info/emoji/:emoji/:size", InfoHandlers::emoji).noAuth().cacheDays(1).desc("Emoji");
		webServer.add("api/info/define/:word", InfoHandlers::define).noAuth().cacheMinutes(1).desc("Dictionary definition");

		webServer.add("api/guilds", GuildHandlers::guilds).cacheMinutes(1).log().desc("Guild list");
		webServer.add("api/guild/info/:guild", GuildHandlers::info).member().cacheHours(1).desc("Get guild info");
		webServer.add("api/guild/banner/:guild", GuildHandlers::banner).noAuth().cacheSeconds(1).desc("Get banner image with basic guild info");
		webServer.add("api/guild/settings/basic/:guild", GuildHandlers::getSettings).member().desc("Get basic guild settings");
		webServer.add("api/guild/settings/basic/:guild/:setting", GuildHandlers::updateSetting).patch().owner().desc("Update basic guild settings");
		webServer.add("api/guild/icon/:guild/:size", GuildHandlers::icon).noAuth().cacheDays(1).desc("Guild icon");
		webServer.add("api/guild/file/:channel/:id/:filename", PasteHandlers::file).noAuth().cacheMinutes(5).desc("Old raw file contents");
		webServer.add("paste/:id/raw", PasteHandlers::pasteRaw).noAuth().cacheMinutes(5).desc("Raw file contents");
		webServer.add("paste/:id", PasteHandlers::paste).noAuth().cacheMinutes(5).desc("Pretty file contents");
		webServer.add("api/guild/feedback/:guild/:id", GuildHandlers::feedback).member().desc("Feedback info");
		webServer.add("api/guild/feedback/:guild", GuildHandlers::feedbackList).member().cacheMinutes(1).desc("Feedback list");
		webServer.add("api/guild/polls/:guild/:id", GuildHandlers::poll).member().desc("Poll info");
		webServer.add("api/guild/polls/:guild", GuildHandlers::pollList).member().desc("Poll list");
		webServer.add("api/guild/members/:guild", GuildHandlers::members).member().cacheMinutes(5).desc("Guild member list");
		webServer.add("api/guild/member/:guild/:member", GuildHandlers::member).member().cacheMinutes(5).desc("Guild member info");
		webServer.add("api/guild/unpingable-names/:guild", GuildHandlers::unpingableNames).admin().desc("Normalized guild member name csv");
		webServer.add("api/guild/audit-log/:guild", GuildHandlers::auditLog).admin().log().desc("Audit Log");
		webServer.add("api/guild/bad-word-regex/:guild", GuildHandlers::badWordRegex).member().log().desc("Bad word regex");
		webServer.add("api/guild/export-messages/:guild/:member", GuildHandlers::exportMessages).admin().log().desc("Export messages by user");

		webServer.add("api/guild/activity/leaderboard/:guild/:days", ActivityHandlers::leaderboard).member().cacheHours(1).desc("Leaderboard");
		webServer.add("api/guild/activity/leaderboard-image/:guild/:days", ActivityHandlers::leaderboardImage).member().cacheHours(1).desc("Leaderboard Image");
		webServer.add("api/guild/activity/members/:guild", ActivityHandlers::members).member().cacheMinutes(5).desc("Member activity");
		webServer.add("api/guild/activity/channels/:guild", ActivityHandlers::channels).member().cacheMinutes(5).desc("Channel activity");

		webServer.add("public/:file", SpecialHandlers::publicfile).noAuth().cacheMinutes(5);

		webServer.add("panel/login", PanelHandlers::login).noAuth().log().desc("Panel Login");
		webServer.add("panel/guilds", PanelHandlers::guilds).cacheMinutes(10).desc("Guild list");

		webServer.addWS("api/cli", CLISession.Handler.INSTANCE);

		webServer.start(Config.get().port);

		info("");
		info("API Endpoints:");
		info("");

		Table table = new Table("Method", "Path", "Auth Level", "Cache", "Description");

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

			cells[4].value(wrapper.description);
		}

		table.print();

		info("");

		ScamHandler.loadDomains();
		DM.loadDmChannels();

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

	@Nullable
	public WebToken getToken(WsConnectContext ctx) {
		String c = ctx.cookie("gnometoken");

		if (c != null && !c.isEmpty()) {
			if (c.equals(Config.get().self_token)) {
				return Utils.selfToken;
			}

			return db.webTokens.findFirst(c);
		}

		return null;
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

	private void printMessageTable(List<Bson> filter, int limit) {
		List<DiscordMessage> messages = new ArrayList<>();

		for (DiscordMessage m : db.guildModdedMC().messages.query().filters(filter).limit(limit).descending("timestamp")) {
			messages.add(m);
		}

		Collections.reverse(messages);

		Table table = new Table("Time", "Channel", "Member", "Content");
		UserCache cache = discordHandler.createUserCache();

		for (DiscordMessage m : messages) {
			Table.Cell[] cells = table.addRow();
			cells[0].value(m.getDate().toInstant());
			cells[1].value(m.getChannelID());
			cells[2].value(cache.get(Snowflake.of(m.getUserID())).get().getUsername());
			cells[3].value(m.getContent().replace("\n", " | "));
		}

		table.print();
		info("Results: " + table.rows.size());
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
			gc.discordJS = new DiscordJS(gc);
		}
	}

	public void deleteGlobalSlashCommand(String id) {
		for (ApplicationCommandData data : discordHandler.getGlobalCommands()) {
			if (data.name().equals(id)) {
				discordHandler.deleteGlobalCommand(Snowflake.of(data.id()));
				return;
			}
		}
	}

	public void deleteGuildSlashCommand(String id, Snowflake guild) {
		for (ApplicationCommandData data : discordHandler.getGuildCommands(guild)) {
			if (data.name().equals(id)) {
				discordHandler.deleteGuildCommand(guild, Snowflake.of(data.id()));
				return;
			}
		}
	}

	public void listGlobalSlashCommands() {
		for (ApplicationCommandData data : discordHandler.getGlobalCommands()) {
			info(data.id() + " : " + data.name());
		}
	}

	public void listGuildSlashCommands(Snowflake guild) {
		for (ApplicationCommandData data : discordHandler.getGuildCommands(guild)) {
			info(data.id() + " : " + data.name());
		}
	}

	public void heapdump() {
		System.gc();

		try {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
			mxBean.getDiagnosticOptions().forEach(App::info);
			String filename = "run/files/heapdump-" + Instant.now().toString().replace(':', '-') + ".hprof";
			mxBean.dumpHeap(filename, false);
			warn("Heap dump saved: " + ((Files.size(AppPaths.DATA_GUILDS.resolve(filename)) / 1024L) / 1024D) + " MB");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void printDatabases() {
		Table table = new Table("DB", "Count");

		db.collections.values().stream().sorted(Comparator.comparing(o -> o.id)).forEach(collection -> {
			Table.Cell[] cells = table.addRow();
			cells[0].value(collection.id);
			cells[1].value(Long.toUnsignedString(collection.count()));
		});

		table.print();
	}

	public void printFonts() {
		for (Font font : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
			info(font.getFontName());
		}
	}

	public void printGuilds(@Nullable String find) {
		if (find != null) {
			info("Looking for: " + find);
		}

		Snowflake findId = find == null || find.isEmpty() ? null : Snowflake.of(find.trim());

		long guilds = 0L;
		long members = 0L;
		Table table = new Table("ID", "Name", "Members", "Gnome Messages");

		for (Guild guild : discordHandler.getSelfGuilds()) {
			long c = 0L;

			if (findId != null) {
				for (Member member : guild.getMembers().toIterable()) {
					if (member.getId().equals(findId)) {
						info("Found " + find.trim() + " in " + guild.getName());
					}

					c++;
				}
			} else {
				c = guild.getMembers().count().block();
			}

			long gm = db.guild(guild.getId()).messages.query().eq("user", discordHandler.selfId.asLong()).count();
			table.addRow(guild.getId().asString(), guild.getName(), c, gm);
			members += c;
			guilds++;
		}

		table.print();
		App.info("Gnome Bot is in " + guilds + " guilds, total " + members + " members");
	}

	public void leaveGuild(Snowflake id) {
		discordHandler.client.getGuildById(id).flatMap(Guild::leave).subscribe();
	}
}