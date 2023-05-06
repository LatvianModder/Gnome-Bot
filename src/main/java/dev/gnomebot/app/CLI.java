package dev.gnomebot.app;

import dev.gnomebot.app.server.WSHandler;
import dev.gnomebot.app.util.CharMap;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.FormattingUtils;
import dev.latvian.apps.webutils.ansi.Ansi;
import dev.latvian.apps.webutils.ansi.Table;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;

import java.util.Scanner;

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
					case "port" -> port();
					case "stats" -> stats();
					case "flags" -> flags(input);
					case "debug" -> debug();
					case "token" -> App.info(Utils.createToken());
					case "leave_guild" -> leaveGuild(Snowflake.of(input[1]));
					case "remove_modifiers" -> App.info(CharMap.MODIFIER_PATTERN.matcher(nonInput).replaceAll(""));
					case "colors" -> colors();
					case "echo_cli" -> echoCli(nonInput);
					case "guilds" -> printGuilds();
					default -> Ansi.log("Unknown command: " + input[0]);
				}
			} catch (IllegalArgumentException ex) {
				Ansi.log(ex.getMessage());
			} catch (Exception ex) {
				Ansi.log(ex);
			}
		}
	}

	private void printThreads() {
		for (var t : Thread.getAllStackTraces().keySet()) {
			Ansi.log("- " + t.getName() + ": " + t.getClass().getName());
		}
	}

	private void port() {
		// app.userDB.updateMany(Filters.not(Filters.exists("avatar_url")), Updates.set("avatar_url", ""));
		//app.userDB.updateMany(Filters.empty(), Updates.unset("online"));
		//app.userDB.updateMany(Filters.ne("name", ""), Updates.rename("name", "custom_name"));
		// app.logDB.collection().renameCollection(new MongoNamespace("hub_beast_dev", "log"));

		/*
		for (var role : app.rolesById.values()) {
			role.flags = RoleFlag.DISCORD.flag;
			app.roleDB.update(role.filter(), Updates.set("flags", role.flags));
		}
		 */

		Ansi.log("Done");
	}

	private void stats() {
		Ansi.log("***");

		Ansi.log("DB Stats:");

		/*
		for (var db : app.db.collections.values()) {
			Ansi.log("- " + db.name + ":");
			Ansi.log("    Size: " + db.count);
			Ansi.log("    Queue: " + db.queue.size());
		}
		 */

		Ansi.log("***");
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
}
