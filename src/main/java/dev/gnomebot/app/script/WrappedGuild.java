package dev.gnomebot.app.script;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.mods.rhino.util.DynamicMap;
import dev.latvian.mods.rhino.util.HideFromJS;
import discord4j.rest.service.GuildService;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WrappedGuild extends DiscordObject {
	public final transient DiscordJS discordJS;
	public final transient GuildCollections gc;
	public final Map<String, WrappedChannel> channels;
	public final Map<String, WrappedRole> roles;
	public final Map<String, WrappedMember> members;

	public WrappedGuild(DiscordJS d, GuildCollections w) {
		super(new WrappedId(w.guildId));
		discordJS = d;
		gc = w;

		channels = new DynamicMap<>(id -> new WrappedChannel(new WrappedId(id), this));
		roles = Collections.unmodifiableMap(gc.getRoleMap().values().stream().map(r -> new WrappedRole(this, r)).collect(Collectors.toMap(k -> k.id.asString(), Function.identity())));
		members = new DynamicMap<>(id -> new WrappedMember(new WrappedId(id), this));
	}

	@Override
	public String toString() {
		return gc.toString();
	}

	@HideFromJS
	public GuildService getGuildService() {
		return gc.getClient().getRestClient().getGuildService();
	}

	public WrappedUser getUser(String id) {
		var m = members.get(id);
		var snowflake = SnowFlake.num(id);

		if (gc.getMember(snowflake) == null) {
			return new WrappedUser(new WrappedId(snowflake), this);
		}

		return m;
	}
}
