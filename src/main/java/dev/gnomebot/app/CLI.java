package dev.gnomebot.app;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.sun.management.HotSpotDiagnosticMXBean;
import dev.gnomebot.app.server.WSHandler;
import dev.gnomebot.app.util.CharMap;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.FormattingUtils;
import dev.latvian.apps.webutils.ansi.Ansi;
import dev.latvian.apps.webutils.ansi.Table;
import discord4j.core.object.entity.Guild;
import org.bson.Document;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
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
					case "token" -> App.info(Utils.createToken());
					case "short_token" -> App.info(Utils.createShortToken());
					case "leave_guild" -> leaveGuild(SnowFlake.num(input[1]));
					case "remove_modifiers" -> App.info(CharMap.MODIFIER_PATTERN.matcher(nonInput).replaceAll(""));
					case "colors" -> colors();
					case "echo_cli" -> echoCli(nonInput);
					case "guilds" -> printGuilds();
					case "scheduled" -> app.printScheduled();
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
		var mxBean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
		mxBean.dumpHeap("heapdump.hprof", false);
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

		App.info(sb);
		App.info("+ Done");
	}

	private void port(String input) throws Exception {
		// var mm = app.db.guild(Snowflake.of(166630061217153024L));
		var count = 0;

		// var database = app.db.mongoClient.getDatabase("gnomebot_test");
		// var collection = database.getCollection("abc");
		// collection.insertOne(new Document("test", 1));
		// collection.renameCollection(new MongoNamespace("gnomebot", "test_collection_124"));

		var regex = Pattern.compile("^[?!]+");
		var filter = Filters.regex("content", Pattern.compile("^[?!]+\\S+"));

		for (var gc : app.db.allGuilds()) {
			App.info("Porting " + gc);
		}

		app.db.channelSettingsDB.getCollection().updateMany(Filters.exists("guild"), Updates.unset("guild"));
		app.db.channelSettingsDB.getCollection().updateMany(Filters.exists("name"), Updates.unset("name"));

		var docs = new ArrayList<Document>();
		app.db.channelSettingsDB.getCollection().find().forEach(docs::add);

		for (var doc : docs) {
			if (doc.size() == 1) {
				app.db.channelSettingsDB.getCollection().deleteOne(Filters.eq(doc.get("_id")));
				count++;
			}
		}

		App.info("+ Done " + count);
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
		var flags = 0L;

		for (var i = 1; i < input.length; i++) {
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

	private void leaveGuild(long id) {
		app.discordHandler.client.getGuildById(SnowFlake.convert(id)).flatMap(Guild::leave).subscribe();
	}

	private void printGuilds() {
		var table = new Table("Name", "Owner", "Members", "Messages", "Gnome Messages", "ID");

		for (var g : app.discordHandler.getSelfGuilds()) {
			App.info("Loading guild " + g.getId().asString() + " " + g.getName() + "...");
			var gc = app.db.guild(g.getId());

			table.addRow(FormattingUtils.trim(g.getName(), 70), gc.getMember(gc.ownerId).getDisplayName(), g.getMembers().count().block(), gc.messages.count(), gc.messages.query().eq("user", app.discordHandler.selfId).count(), g.getId().asString());
		}

		table.print();
	}

	private void colors() {
		var line1 = new StringBuilder();
		var line2 = new StringBuilder();

		for (var i = 0; i < 8; i++) {
			line1.append("\u001B[3").append(i).append("m■ Hello ");
			line2.append("\u001B[9").append(i).append("m■ Hello ");
		}

		App.info(line1);
		App.info(line2);

		for (var y = 0; y < 16; y++) {
			var str = new StringBuilder();

			for (var x = 0; x < 16; x++) {
				str.append("\u001B[48;5;").append(x + y * 16).append("m  ");
			}

			App.info(str);
		}
	}

	private void echoCli(String message) {
		App.info("Sending to all CLI clients: " + message);
		WSHandler.CLI.broadcast(message);
	}
}
