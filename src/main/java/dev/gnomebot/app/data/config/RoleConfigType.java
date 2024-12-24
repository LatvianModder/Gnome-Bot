package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.util.SnowFlake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class RoleConfigType implements SnowflakeConfigType<RoleConfigType.Holder> {
	public static final RoleConfigType DEFAULT = new RoleConfigType();

	public static class Holder extends ConfigHolder<Long> {
		public Holder(GuildCollections gc, ConfigKey<Long, Holder> key) {
			super(gc, key);
		}

		@Override
		public String toString() {
			return "<@&" + get() + '>';
		}

		public boolean isSet() {
			return get() != 0L;
		}

		@Nullable
		public CachedRole getRole() {
			return isSet() ? gc.roles().get(get()) : null;
		}

		public Optional<CachedRole> role() {
			return isSet() ? Optional.ofNullable(getRole()) : Optional.empty();
		}

		public boolean is(Member member) {
			return isSet() && member.getRoleIds().contains(SnowFlake.convert(get()));
		}

		public boolean isMentioned(Message message) {
			return isSet() && message.getRoleMentionIds().contains(SnowFlake.convert(get()));
		}
	}

	@Override
	public String getTypeName() {
		return "role";
	}

	@Override
	public Holder createHolder(GuildCollections gc, ConfigKey<Long, Holder> key) {
		return new Holder(gc, key);
	}

	@Override
	public String validate(GuildCollections guild, int type, String value) {
		return !value.isEmpty() && (!value.startsWith("@") && guild.roles().get(SnowFlake.num(value)) != null || guild.roles().uniqueNameMap.containsKey(value.substring(1))) ? "" : "Role not found!";
	}

	@Override
	public boolean hasEnumValues() {
		return true;
	}

	@Override
	public Collection<EnumValue> getEnumValues(GuildCollections guild) {
		var list = new ArrayList<EnumValue>();

		for (var entry : guild.roles().uniqueNameMap.entrySet()) {
			list.add(new EnumValue(SnowFlake.str(entry.getValue().id), "@" + entry.getKey()));
		}

		return list;
	}

	@Override
	public String serialize(GuildCollections guild, int type, Long value) {
		for (var entry : guild.roles().uniqueNameMap.entrySet()) {
			if (entry.getValue().id == value) {
				return "@" + entry.getKey();
			}
		}

		return "";
	}

	@Override
	public Long deserialize(GuildCollections guild, int type, String value) {
		if (value.isEmpty()) {
			return 0L;
		} else if (value.startsWith("@")) {
			var role = guild.roles().uniqueNameMap.get(value.substring(1));
			return role == null ? 0L : role.id;
		} else {
			return SnowFlake.num(value);
		}
	}
}
