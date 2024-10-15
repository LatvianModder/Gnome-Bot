package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.util.SimpleStringReader;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.webutils.data.Pair;
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
		var ns = readString();

		if (ns.isEmpty()) {
			return Optional.empty();
		}

		var s = ns.get();

		if (s.equalsIgnoreCase("Gnome")) {
			return Optional.of(gc.db.app.discordHandler.getSelfUser());
		}

		if (s.startsWith("<@") && s.endsWith(">")) {
			s = s.substring(2, s.length() - 1);

			if (s.startsWith("!")) {
				s = s.substring(1);
			}
		}

		var lh = s.lastIndexOf('#');

		if (lh > 0 && s.indexOf('@') == 0) {
			var s0 = s.substring(1, lh);
			var s1 = s.substring(lh + 1);
			Log.info(s0 + " # " + s1);
			var member = gc.members.query().eq("name", s0).eq("discriminator", s1).first();

			if (member != null) {
				var user = gc.db.app.discordHandler.getUser(member.getUID());

				if (user != null) {
					return Optional.of(user);
				}
			}

			throw new GnomeException("User not found!");
		}

		try {
			return Optional.of(gc.db.app.discordHandler.getUser(SnowFlake.num(s)));
		} catch (Exception ex) {
			var member = gc.members.query().eq("name", s).first();

			if (member == null) {
				member = gc.members.query().exists("nickname").regex("nickname", Pattern.compile("^" + s + "$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)).first();
			}

			if (member == null) {
				member = gc.members.query().exists("nickname").regex("nickname", Pattern.compile(s, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.LITERAL)).first();
			}

			if (member != null) {
				var user = gc.db.app.discordHandler.getUser(member.getUID());

				if (user != null) {
					return Optional.of(user);
				}
			}

			throw new GnomeException("User not found!");
		}
	}

	public Optional<ChannelInfo> readChannelInfo() {
		var ns = readString();

		if (ns.isEmpty()) {
			return Optional.empty();
		}

		var s = ns.get();

		if (s.startsWith("<#") && s.endsWith(">")) {
			s = s.substring(2, s.length() - 1);
		}

		try {
			return Optional.of(gc.getOrMakeChannelInfo(SnowFlake.num(s)));
		} catch (Exception ignored) {
			throw new GnomeException("Channel not found!");
		}
	}

	public Optional<Pair<ChannelInfo, Long>> readChannelAndMessage() {
		var ns = readString();

		if (ns.isEmpty()) {
			return Optional.empty();
		}

		var s = ns.get().split("[/:]", 2);

		Log.info(Arrays.toString(s));

		if (s[0].startsWith("<#") && s[0].endsWith(">")) {
			s[0] = s[0].substring(2, s[0].length() - 1);
		}

		try {
			var ci = gc.getChannelInfo(SnowFlake.num(s[0]));
			var li = ci == null ? 0L : ci.getLastMessageId();

			if (li != 0L) {
				return Optional.of(Pair.of(ci, s.length == 2 ? SnowFlake.num(s[1]) : li));
			}
		} catch (Exception ignored) {
		}

		return Optional.empty();
	}

	public Optional<CachedRole> readRole() {
		var ns = readString();

		if (ns.isEmpty()) {
			return Optional.empty();
		}

		var s = ns.get();

		if (s.startsWith("<@&") && s.endsWith(">")) {
			s = s.substring(3, s.length() - 1);
		}

		try {
			var snowflake = SnowFlake.num(s);

			if (snowflake == gc.guildId) {
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

			for (var r : gc.getRoleList()) {
				if (r.name.equals(s)) {
					return Optional.of(r);
				}
			}

			s = s.replaceAll("\\W", "").toLowerCase();

			for (var r : gc.getRoleList()) {
				if (r.name.replaceAll("\\W", "").toLowerCase().equals(s)) {
					return Optional.of(r);
				}
			}

			for (var r : gc.getRoleList()) {
				if (r.name.replaceAll("\\W", "").toLowerCase().contains(s)) {
					return Optional.of(r);
				}
			}

			throw new GnomeException("Role not found!");
		}
	}
}