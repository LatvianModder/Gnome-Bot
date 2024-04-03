package dev.gnomebot.app;

import com.sun.management.HotSpotDiagnosticMXBean;
import dev.gnomebot.app.server.WSHandler;
import dev.gnomebot.app.util.CharMap;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.FormattingUtils;
import dev.latvian.apps.webutils.ansi.Log;
import dev.latvian.apps.webutils.ansi.Table;
import discord4j.core.object.entity.Guild;

import java.lang.management.ManagementFactory;
import java.util.Scanner;

public class CLI extends Thread {
	public final App app;

	public CLI(App app) {
		this.app = app;
		setDaemon(true);
	}

	@Override
	public void run() {
		var scanner = new Scanner(System.in);

		while (app.running) {
			var input = scanner.nextLine().split(" ");
			var nonInput = input.length == 1 ? "" : String.join(" ", input).substring(input[0].length() + 1).trim();

			try {
				switch (input[0]) {
					case "restart" -> app.restart();
					case "reload" -> app.reload();
					case "threads" -> printThreads();
					case "dump" -> dump();
					case "size" -> size();
					case "port" -> port(nonInput);
					case "stats" -> stats();
					case "flags" -> flags(input);
					case "debug" -> debug();
					case "token" -> Log.info(Utils.createToken());
					case "short_token" -> Log.info(Utils.createShortToken());
					case "leave_guild" -> leaveGuild(SnowFlake.num(input[1]));
					case "remove_modifiers" -> Log.info(CharMap.MODIFIER_PATTERN.matcher(nonInput).replaceAll(""));
					case "echo_cli" -> echoCli(nonInput);
					case "guilds" -> printGuilds();
					default -> Log.warn("Unknown command: " + input[0]);
				}
			} catch (IllegalArgumentException ex) {
				Log.error(ex.getMessage());
			} catch (Exception ex) {
				Log.error(ex);
			}
		}
	}

	private void printThreads() {
		for (var t : Thread.getAllStackTraces().keySet()) {
			Log.info("- " + t.getName() + ": " + t.getClass().getName());
		}
	}

	private void dump() throws Exception {
		var server = ManagementFactory.getPlatformMBeanServer();
		var mxBean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
		mxBean.dumpHeap("heapdump.hprof", false);
		Log.info("Heap Dump");
	}

	private void size() throws Exception {
		System.out.print("\u001b[s\u001b[5000;5000H\u001b[6n\u001b[u");

		var sb = new StringBuilder();

		while (true) {
			var c = System.in.read();

			if (c == 'R') {
				break;
			}

			sb.append((char) c);
		}

		Log.info(sb);
		Log.success("Done");
	}

	private void port(String input) throws Exception {
		// var mm = app.db.guild(Snowflake.of(166630061217153024L));
		var count = 0L;

		// var database = app.db.mongoClient.getDatabase("gnomebot_test");
		// var collection = database.getCollection("abc");
		// collection.insertOne(new Document("test", 1));
		// collection.renameCollection(new MongoNamespace("gnomebot", "test_collection_124"));

		for (var gc : app.db.allGuilds()) {
			Log.info("Porting " + gc);

			Log.warn("Done %,d".formatted(count));
		}

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

	private void flags(String[] input) {
		var flags = 0L;

		for (var i = 1; i < input.length; i++) {
			try {
				flags |= 1L << Long.parseLong(input[i]);
			} catch (NumberFormatException ex) {
				Log.info("Invalid number: " + input[i]);
			}
		}

		Log.info("%016X: %d".formatted(flags, flags));
	}

	private void debug() {
		App.debug = !App.debug;
		Log.info("+ Debug mode: " + (App.debug ? "enabled" : "disabled"));
	}

	private void leaveGuild(long id) {
		app.discordHandler.client.getGuildById(SnowFlake.convert(id)).flatMap(Guild::leave).subscribe();
	}

	private void printGuilds() {
		var table = new Table("Name", "Owner", "Members", "Messages", "Gnome Messages", "ID");

		for (var g : app.discordHandler.getSelfGuilds()) {
			Log.info("Loading guild " + g.getId().asString() + " " + g.getName() + "...");
			var gc = app.db.guild(g.getId());

			table.addRow(FormattingUtils.trim(g.getName(), 70), gc.getMember(gc.ownerId).getDisplayName(), g.getMembers().count().block(), gc.messages.count(), gc.messages.query().eq("user", app.discordHandler.selfId).count(), g.getId().asString());
		}

		table.print();
	}

	private void echoCli(String message) {
		Log.info("Sending to all CLI clients: " + message);
		WSHandler.CLI.broadcast(message);
	}
}
