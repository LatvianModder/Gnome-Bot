package dev.gnomebot.app.script;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.GuildCollections;
import dev.latvian.mods.rhino.util.DynamicMap;
import dev.latvian.mods.rhino.util.HideFromJS;
import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.UserData;
import discord4j.rest.service.GuildService;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class WrappedGuild implements WithId {
	public final transient GuildCollections guild;
	public final WrappedId id;
	public final Map<String, WrappedChannel> channels;
	public final Map<String, WrappedRole> roles;
	public final Map<String, WrappedMember> members;

	public WrappedGuild(GuildCollections w) {
		guild = w;
		id = new WrappedId(guild.guildId);

		channels = new DynamicMap<>(id -> {
			Snowflake snowflake = Snowflake.of(id);
			WrappedChannel c = new WrappedChannel(this, new WrappedId(snowflake));
			c.update(guild.getChannelMap().get(snowflake));
			return c;
		});

		roles = Collections.unmodifiableMap(guild.getRoleMap().values().stream().map(r -> new WrappedRole(this, r)).collect(Collectors.toMap(k -> k.id.asString, v -> v)));
		members = new DynamicMap<>(id -> {
			Snowflake snowflake = Snowflake.of(id);
			return new WrappedMember(this, new WrappedId(snowflake), Objects.requireNonNull(guild.getMember(snowflake)));
		});

		for (ChannelInfo ci : guild.getChannelList()) {
			channels.get(ci.id.asString()).update(ci);
		}
	}

	@Override
	public WrappedId id() {
		return id;
	}

	@HideFromJS
	public GuildService getGuildService() {
		return guild.getClient().getRestClient().getGuildService();
	}

	public WrappedUser getUser(String id) {
		WrappedMember m = members.get(id);

		if (m.member == null) {
			Snowflake snowflake = Snowflake.of(id);
			WrappedUser u = new WrappedUser(this, new WrappedId(snowflake));

			try {
				UserData userData = guild.db.app.discordHandler.getUserData(snowflake);

				if (userData != null) {
					u.update(userData);
					return u;
				}
			} catch (Exception ex) {
			}

			return u;
		}

		return m;
	}
}
