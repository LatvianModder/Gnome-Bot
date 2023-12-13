package dev.gnomebot.app;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.sun.management.HotSpotDiagnosticMXBean;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.server.WSHandler;
import dev.gnomebot.app.util.CharMap;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.FormattingUtils;
import dev.latvian.apps.webutils.ansi.Ansi;
import dev.latvian.apps.webutils.ansi.Table;
import dev.latvian.apps.webutils.data.Mutable;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.ThreadChannel;
import org.bson.Document;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class CLI extends Thread {
	public final App app;

	public CLI(App app) {
		this.app = app;
		setDaemon(true);
	}

	@Override
	public void run() {
		Scanner scanner = new Scanner(System.in);

		while (app.running) {
			var input = scanner.nextLine().split(" ");
			var nonInput = input.length == 1 ? "" : String.join(" ", input).substring(input[0].length() + 1).trim();

			try {
				switch (input[0]) {
					case "restart" -> app.restart();
					case "reload" -> app.reload();
					case "threads" -> printThreads();
					case "dump" -> dump();
					case "port" -> port(nonInput);
					case "stats" -> stats();
					case "flags" -> flags(input);
					case "debug" -> debug();
					case "token" -> App.info(Utils.createToken());
					case "short_token" -> App.info(Utils.createShortToken());
					case "leave_guild" -> leaveGuild(Snowflake.of(input[1]));
					case "remove_modifiers" -> App.info(CharMap.MODIFIER_PATTERN.matcher(nonInput).replaceAll(""));
					case "colors" -> colors();
					case "echo_cli" -> echoCli(nonInput);
					case "guilds" -> printGuilds();
					case "tt1" -> testThread1(Snowflake.of(input[1]));
					case "tt2" -> testThread2(nonInput);
					case "scheduled" -> app.printScheduled();
					case "close_thread" -> closeThread(Snowflake.of(input[1]), Integer.parseInt(input[2]));
					case "guild_config" -> guildConfig(Snowflake.of(input[1]), input[2], input[3]);
					default -> Ansi.log("Unknown command: " + input[0]);
				}
			} catch (IllegalArgumentException ex) {
				App.error(ex.getMessage());
			} catch (Exception ex) {
				App.error(ex);
			}
		}
	}

	private void printThreads() {
		for (var t : Thread.getAllStackTraces().keySet()) {
			App.info("- " + t.getName() + ": " + t.getClass().getName());
		}
	}

	private void dump() throws Exception {
		var server = ManagementFactory.getPlatformMBeanServer();
		HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
		mxBean.dumpHeap("heapdump.hprof", false);
	}

	private record Trick(String id, boolean clj, long author, long created, Mutable<String> content) {
	}

	private void port(String input) throws Exception {
		var guild = app.db.guild(Snowflake.of(166630061217153024L));

		for (var msg : guild.messages.getCollection().aggregate(List.of(
				Aggregates.match(Filters.regex("content", Pattern.compile("^\\?\\?"))),
				Aggregates.group("$content", Accumulators.sum("count", 1)),
				Aggregates.match(Filters.gt("count", 1)),
				Aggregates.sort(Sorts.ascending("count"))
		))) {
			var macro = guild.getMacro(msg.getString("_id").substring(2));

			if (macro != null) {
				macro.update("uses", msg.getInteger("count"));
				App.info(macro.getName() + ": " + macro.getUses());
			}
		}

		App.info("+ Done");
	}

	private void portTricks(GuildCollections guild) throws Exception {
		guild.macros.getCollection().deleteMany(Filters.eq("k9", true));

		var count = 0;
		var pattern = Pattern.compile("^[?!]+trick\\s+-([aurt]+)([=\\s]clj|\\s+--type[=\\s]clj)?\\s+(\".+\"|\\S+)\\s+(.+)", Pattern.MULTILINE);

		var map = new LinkedHashMap<String, Trick>();

		var admins = new HashSet<Long>();
		var adminRole = guild.adminRole.getRole().id;

		for (var member : guild.getMembers()) {
			if (member.getRoleIds().contains(adminRole)) {
				admins.add(member.getId().asLong());
			}
		}

		admins.add(140245257416736769L); // tt
		admins.add(105002867424796672L); // tehnut
		admins.add(107511731999244288L); // simon
		admins.add(124923088067362817L); // awade

		for (var msg : guild.messages.query().regex("content", Pattern.compile("^[?!]+trick -[aur]")).sort(Sorts.ascending("timestamp")).projectionFields("_id", "content", "user")) {
			var matcher = pattern.matcher(msg.getContent());

			if (matcher.find()) {
				count++;

				long author = msg.getUserID();
				var type = matcher.group(1);
				boolean clj = matcher.group(2) != null;
				var trick = matcher.group(3);

				if (trick.length() >= 2 && trick.startsWith("\"") && trick.endsWith("\"")) {
					trick = trick.substring(1, trick.length() - 1);
				}

				if (trick.isEmpty()) {
					continue;
				}

				var content = msg.getContent()
						.substring(matcher.start(4))
						.trim()
						.replaceAll("<@&(\\d+)>", "role:$1")
						.replaceAll("<@(\\d+)>", "user:$1")
						.replace("@here", "mention:here")
						.replace("@everyone", "mention:everyone");

				if (content.isEmpty()) {
					continue;
				}

				if (type.indexOf('a') != -1) {
					map.put(trick.toLowerCase(), new Trick(trick, clj, author, Snowflake.of(msg.getUID()).getTimestamp().toEpochMilli(), new Mutable<>(content)));
				} else if (type.indexOf('u') != -1) {
					var t = map.get(trick.toLowerCase());

					if (t != null && (t.author == author || admins.contains(author))) {
						t.content.value = content;
					}
				} else if (type.indexOf('r') != -1) {
					var t = map.get(trick.toLowerCase());

					if (t != null && (t.author == author || admins.contains(author))) {
						map.remove(trick.toLowerCase());
					}
				}
			}
		}

		var list = new ArrayList<String>();

		for (var trick : map.values()) {
			list.add((trick.clj ? "* " : "# ") + trick.id + " " + Long.toUnsignedString(trick.author) + " " + Long.toUnsignedString(trick.created));

			for (var str : trick.content.value.split("\n")) {
				list.add("> " + str);
			}

			list.add("");

			if (guild.getMacro(trick.id) == null) {
				Document document = new Document();
				document.put("name", trick.id);
				document.put("content", trick.content.value);
				document.put("author", trick.author);
				document.put("created", new Date(trick.created));
				document.put("uses", 0);
				document.put("type", trick.clj ? "clj" : "text");
				document.put("k9", true);
				guild.macros.insert(document);
				App.info("~ Added " + trick.id);
			}
		}

		Files.write(Path.of("export1.txt"), list);

		guild.updateMacroMap();

		App.info("+ " + count + " messages found");
		App.info("+ " + map.size() + " tricks found");
	}

	private void stats() {
		App.info("***");

		App.info("DB Stats:");

		/*
		for (var db : app.db.collections.values()) {
			Ansi.log("- " + db.name + ":");
			Ansi.log("    Size: " + db.count);
			Ansi.log("    Queue: " + db.queue.size());
		}
		 */

		App.info("***");
	}

	private void flags(String[] input) {
		long flags = 0L;

		for (int i = 1; i < input.length; i++) {
			try {
				flags |= 1L << Long.parseLong(input[i]);
			} catch (NumberFormatException ex) {
				Ansi.log("Invalid number: " + input[i]);
			}
		}

		Ansi.log("%016X: %d".formatted(flags, flags));
	}

	private void debug() {
		App.debug = !App.debug;
		App.success("Debug mode: " + (App.debug ? "enabled" : "disabled"));
	}

	private void leaveGuild(Snowflake id) {
		app.discordHandler.client.getGuildById(id).flatMap(Guild::leave).subscribe();
	}

	private void printGuilds() {
		var table = new Table("Name", "Owner", "Members", "Messages", "Gnome Messages", "ID");

		for (Guild g : app.discordHandler.getSelfGuilds()) {
			App.info("Loading guild " + g.getId().asString() + " " + g.getName() + "...");
			var gc = app.db.guild(g.getId());

			table.addRow(FormattingUtils.trim(g.getName(), 70), gc.getMember(g.getOwnerId()).getDisplayName(), g.getMembers().count().block(), gc.messages.count(), gc.messages.query().eq("user", app.discordHandler.selfId.asLong()).count(), g.getId().asString());
		}

		table.print();
	}

	private void colors() {
		var line1 = new StringBuilder();
		var line2 = new StringBuilder();

		for (int i = 0; i < 8; i++) {
			line1.append("\u001B[3").append(i).append("m■ Hello ");
			line2.append("\u001B[9").append(i).append("m■ Hello ");
		}

		App.info(line1);
		App.info(line2);

		for (int y = 0; y < 16; y++) {
			var str = new StringBuilder();

			for (int x = 0; x < 16; x++) {
				str.append("\u001B[48;5;").append(x + y * 16).append("m  ");
			}

			App.info(str);
		}
	}

	private void echoCli(String message) {
		App.info("Sending to all CLI clients: " + message);
		WSHandler.CLI.broadcast(message);
	}

	private ThreadChannel threadChannel = null;

	private void testThread1(Snowflake id) {
		var gc = app.db.guild(Snowflake.of(166630061217153024L));
		var userData = app.discordHandler.getUserData(id);
		threadChannel = app.discordHandler.client.getChannelById(gc.memberAuditLogThread(userData)).cast(ThreadChannel.class).block();
		App.info(threadChannel.getName());
	}

	private void testThread2(String msg) {
		threadChannel.createMessage(msg).subscribe();
	}

	private void closeThread(Snowflake threadId, int seconds) {
		// close_thread 1111610188584198145 60

		var channel = app.discordHandler.client.getChannelById(threadId).cast(ThreadChannel.class).block();
		var gc = app.db.guild(channel.getGuildId());
		gc.closeThread(threadId, Duration.ofSeconds(seconds));
	}

	private void guildConfig(Snowflake guildId, String config, String value) {
		// guild_config 719235642579746827 global_xp 10
		var c = app.db.guild(guildId).config.map.get(config);
		c.deserialize(value);
		c.save();
	}
}
