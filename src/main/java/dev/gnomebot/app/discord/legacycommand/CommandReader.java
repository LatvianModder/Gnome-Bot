package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.DiscordMember;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.util.SimpleStringReader;
import dev.latvian.apps.webutils.data.Pair;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

public class CommandReader extends SimpleStringReader {
	private final GuildCollections gc;

	public CommandReader(GuildCollections g, String s) {
		super(s);
		gc = g;
	}

	public Optional<User> readUser() {
		Optional<String> ns = readString();

		if (ns.isEmpty()) {
			return Optional.empty();
		}

		String s = ns.get();

		if (s.equalsIgnoreCase("Gnome")) {
			return Optional.of(gc.db.app.discordHandler.getSelfUser());
		}

		if (s.startsWith("<@") && s.endsWith(">")) {
			s = s.substring(2, s.length() - 1);

			if (s.startsWith("!")) {
				s = s.substring(1);
			}
		}

		int lh = s.lastIndexOf('#');

		if (lh > 0 && s.indexOf('@') == 0) {
			String s0 = s.substring(1, lh);
			String s1 = s.substring(lh + 1);
			App.info(s0 + " # " + s1);
			DiscordMember member = gc.members.query().eq("name", s0).eq("discriminator", s1).first();

			if (member != null) {
				User user = gc.db.app.discordHandler.getUser(Snowflake.of(member.getUID()));

				if (user != null) {
					return Optional.of(user);
				}
			}

			throw new GnomeException("User not found!");
		}

		try {
			return Optional.of(gc.db.app.discordHandler.getUser(Snowflake.of(s)));
		} catch (Exception ex) {
			DiscordMember member = gc.members.query().eq("name", s).first();

			if (member == null) {
				member = gc.members.query().exists("nickname").regex("nickname", Pattern.compile("^" + s + "$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)).first();
			}

			if (member == null) {
				member = gc.members.query().exists("nickname").regex("nickname", Pattern.compile(s, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.LITERAL)).first();
			}

			if (member != null) {
				User user = gc.db.app.discordHandler.getUser(Snowflake.of(member.getUID()));

				if (user != null) {
					return Optional.of(user);
				}
			}

			throw new GnomeException("User not found!");
		}
	}

	public Optional<ChannelInfo> readChannelInfo() {
		Optional<String> ns = readString();

		if (ns.isEmpty()) {
			return Optional.empty();
		}

		String s = ns.get();

		if (s.startsWith("<#") && s.endsWith(">")) {
			s = s.substring(2, s.length() - 1);
		}

		try {
			return Optional.of(gc.getOrMakeChannelInfo(Snowflake.of(s)));
		} catch (Exception ex) {
		}

		throw new GnomeException("Channel not found!");
	}

	public Optional<Pair<ChannelInfo, Snowflake>> readChannelAndMessage() {
		Optional<String> ns = readString();

		if (ns.isEmpty()) {
			return Optional.empty();
		}

		String[] s = ns.get().split("[/:]", 2);

		App.info(Arrays.toString(s));

		if (s[0].startsWith("<#") && s[0].endsWith(">")) {
			s[0] = s[0].substring(2, s[0].length() - 1);
		}

		try {
			ChannelInfo ci = gc.getChannelInfo(Snowflake.of(s[0]));
			Snowflake li = ci == null ? null : ci.getLastMessageId();

			if (li != null) {
				return Optional.of(Pair.of(ci, s.length == 2 ? Snowflake.of(s[1]) : li));
			}
		} catch (Exception ex) {
		}

		return Optional.empty();
	}

	public Optional<CachedRole> readRole() {
		Optional<String> ns = readString();

		if (ns.isEmpty()) {
			return Optional.empty();
		}

		String s = ns.get();

		if (s.startsWith("<@&") && s.endsWith(">")) {
			s = s.substring(3, s.length() - 1);
		}

		try {
			Snowflake snowflake = Snowflake.of(s);

			if (snowflake.asLong() == gc.guildId.asLong()) {
				return Optional.empty();
			}

			return Optional.of(gc.getRoleMap().get(snowflake));
		} catch (Exception ex) {
			if (s.startsWith("@")) {
				s = s.substring(1);
			}

			if (s.equals("everyone")) {
				return Optional.empty();
			}

			for (CachedRole r : gc.getRoleList()) {
				if (r.name.equals(s)) {
					return Optional.of(r);
				}
			}

			s = s.replaceAll("\\W", "").toLowerCase();

			for (CachedRole r : gc.getRoleList()) {
				if (r.name.replaceAll("\\W", "").toLowerCase().equals(s)) {
					return Optional.of(r);
				}
			}

			for (CachedRole r : gc.getRoleList()) {
				if (r.name.replaceAll("\\W", "").toLowerCase().contains(s)) {
					return Optional.of(r);
				}
			}

			throw new GnomeException("Role not found!");
		}
	}
}