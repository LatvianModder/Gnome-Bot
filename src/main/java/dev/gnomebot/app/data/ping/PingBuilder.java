package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.data.Databases;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.latvian.apps.webutils.FormattingUtils;
import discord4j.common.util.Snowflake;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

public class PingBuilder {
	public String name;
	public final LinkedHashSet<Snowflake> ignoredGuilds = new LinkedHashSet<>();
	public final LinkedHashSet<Snowflake> ignoredChannels = new LinkedHashSet<>();
	public final LinkedHashSet<Snowflake> ignoredUsers = new LinkedHashSet<>();
	public boolean bots = true;
	public boolean self = false;
	public final List<Ping> pings = new ArrayList<>();

	private void set(LinkedHashSet<Snowflake> list, boolean add, String id) {
		try {
			Snowflake s = Snowflake.of(id);

			if (add) {
				list.add(s);
			} else {
				list.remove(s);
			}
		} catch (Exception e) {
			throw new GnomeException("Invalid snowflake: " + id);
		}
	}

	private PingBuilder copy() {
		PingBuilder builder = new PingBuilder();
		builder.ignoredGuilds.addAll(ignoredGuilds);
		builder.ignoredChannels.addAll(ignoredChannels);
		builder.ignoredUsers.addAll(ignoredUsers);
		builder.bots = bots;
		builder.self = self;
		return builder;
	}

	public UserPingConfig buildConfig() {
		return UserPingConfig.get(ignoredGuilds, ignoredChannels, ignoredUsers, bots, self);
	}

	public UserPingInstance buildInstance(Snowflake user, PingDestination destination) {
		return new UserPingInstance(pings.toArray(Ping.NO_PINGS), user, destination, buildConfig());
	}

	@Override
	public String toString() {
		return "{" +
				"name='" + name + '\'' +
				", config=" + buildConfig() +
				", pings=" + pings +
				'}';
	}

	public static List<PingBuilder> compile(Databases db, Snowflake userId, String config, boolean checkDM) {
		int lineno = 0;
		var list = new ArrayList<PingBuilder>();

		if (config.isEmpty()) {
			return list;
		}

		try {
			var root = new PingBuilder();
			PingBuilder group = null;
			PingBuilder current = root;

			for (var line : config.split("\n")) {
				lineno++;
				line = line.trim();

				if (line.isEmpty()) {
					continue;
				}

				char c = line.charAt(0);

				if (c == '#') {
					continue;
				}

				if (line.length() < 3 || line.charAt(1) != ' ') {
					throw new GnomeException("Second symbol must be a space");
				}

				switch (c) {
					case '+', '-' -> {
						String s = line.substring(2);

						if (s.startsWith("/")) {
							if (s.lastIndexOf('/') < 2) {
								throw new GnomeException("RegEx must end with /[flags]!");
							}

							Pattern pattern = FormattingUtils.parseSafeRegEx(s, 0);

							if (pattern == null) {
								throw new GnomeException("Invalid RegEx!");
							}

							if (group == null) {
								throw new GnomeException("No @ group has been set yet!");
							}

							current.pings.add(new Ping(pattern, c == '+'));
						} else if (s.startsWith("\"")) {
							if (s.length() < 3 || s.charAt(s.length() - 1) != '"') {
								throw new GnomeException("String must end with \"!");
							}

							current.pings.add(new Ping(Pattern.compile("\\b" + s.substring(1, s.length() - 1).replaceAll("([.?$^!+*\\[\\]{}<>()\\\\])", "\\\\$1") + "\\b", Pattern.CASE_INSENSITIVE), c == '+'));
						} else {
							String[] s1 = s.split(" ", 2);

							switch (s1[0]) {
								case "bots" -> current.bots = c == '+';
								case "self" -> current.self = c == '+';
								case "guild" -> current.set(current.ignoredGuilds, c == '-', s1[1]);
								case "channel" -> current.set(current.ignoredChannels, c == '-', s1[1]);
								case "user" -> current.set(current.ignoredUsers, c == '-', s1[1]);
								default -> throw new GnomeException("Unknown flag!");
							}
						}
					}
					case '@' -> {
						if (group != null && !group.pings.isEmpty()) {
							list.add(group);
						}

						group = root.copy();
						group.name = line.substring(2);
						current = group;

						if (group.name.equals("dm")) {
							try {
								DM.openId(db.app.discordHandler, userId);
							} catch (Exception ignored) {
								throw new GnomeException("You must message <@" + db.app.discordHandler.selfId.asString() + "> first before you can use DM channel!");
							}
						} else if (db.userWebhooks.query().eq("user", userId.asLong()).eq("name", group.name).first() == null) {
							throw new GnomeException("Unknown webhook '" + group.name + "'! Set it up with `/webhook add`");
						}
					}
					default -> throw new GnomeException("Invalid character '" + c + "'");
				}
			}

			if (group != null && !group.pings.isEmpty()) {
				list.add(group);
			}
		} catch (GnomeException ex) {
			throw ex.pos(lineno);
		} catch (Exception ex) {
			throw new GnomeException(ex.getMessage()).pos(lineno);
		}

		return list;
	}
}
