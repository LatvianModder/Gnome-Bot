package dev.gnomebot.app.script;

import dev.gnomebot.app.data.GuildCollections;
import dev.latvian.mods.rhino.util.DynamicMap;
import dev.latvian.mods.rhino.util.HideFromJS;
import discord4j.common.util.Snowflake;
import discord4j.rest.service.GuildService;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
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

		channels = new DynamicMap<>(id -> new WrappedChannel(this, new WrappedId(Snowflake.of(id))));
		roles = Collections.unmodifiableMap(guild.getRoleMap().values().stream().map(r -> new WrappedRole(this, r)).collect(Collectors.toMap(k -> k.id.asString(), Function.identity())));
		members = new DynamicMap<>(id -> new WrappedMember(this, new WrappedId(Snowflake.of(id))));
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
		Snowflake snowflake = Snowflake.of(id);

		if (guild.getMember(snowflake) == null) {
			return new WrappedUser(this, new WrappedId(snowflake));
		}

		return m;
	}
}
