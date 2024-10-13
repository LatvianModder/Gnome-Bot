package dev.gnomebot.app;

import com.google.gson.JsonArray;
import com.sun.management.HotSpotDiagnosticMXBean;
import dev.gnomebot.app.data.RelatedGuild;
import dev.gnomebot.app.server.WSHandler;
import dev.gnomebot.app.util.CharMap;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.FormattingUtils;
import dev.latvian.apps.webutils.ansi.Log;
import dev.latvian.apps.webutils.ansi.Table;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
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
		System.out.print("\u001b[s\u001b[9999;9999H\u001b[6n\u001b[u");

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
		var mm = app.db.guild(Snowflake.of(166630061217153024L));
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
					var inv = app.discordHandler.client.getRestClient().getInviteService().getInvite(invite).block();
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
		var json = new JsonArray();
		json.add(group.toJson());
		Files.writeString(mm.paths.path.resolve("related_guilds.json"), json.toString());

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
