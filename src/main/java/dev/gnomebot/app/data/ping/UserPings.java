package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.data.WrappedCollection;
import dev.gnomebot.app.data.WrappedDocument;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class UserPings extends WrappedDocument<UserPings> {
	public UserPings(WrappedCollection<UserPings> c, MapWrapper d) {
		super(c, d);
	}

	public String getConfig() {
		return document.getString("config");
	}

	public List<PingBuilder> createBuilders() {
		return compile(getConfig());
	}

	public static class PingBuilder {
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
	}

	public static List<PingBuilder> compile(String config) {
		int lineno = 0;
		List<PingBuilder> list = new ArrayList<>();

		if (config.isEmpty()) {
			return list;
		}

		try {
			PingBuilder root = new PingBuilder();
			PingBuilder group = null;
			PingBuilder current = root;

			for (String line : config.split("\n")) {
				lineno++;
				line = line.trim();

				if (line.isEmpty()) {
					continue;
				}

				char c = line.charAt(0);

				if (line.length() < 3 || line.charAt(1) != ' ') {
					throw new GnomeException("Second symbol must be a space");
				}

				switch (c) {
					case '#' -> {
					}
					case '+', '-' -> {
						String s = line.substring(2);

						if (s.startsWith("/")) {
							Pattern pattern = Utils.parseSafeRegEx(s, 0);

							if (pattern == null) {
								throw new GnomeException("Invalid RegEx!");
							}

							if (group == null) {
								throw new GnomeException("No @ group has been set yet!");
							}

							current.pings.add(new Ping(pattern, c == '+'));
						} else {
							String[] s1 = s.split(" ");

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