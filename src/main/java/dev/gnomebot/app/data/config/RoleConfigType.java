package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class RoleConfigType implements SnowflakeConfigType<RoleConfigType.Holder> {
	public static final RoleConfigType DEFAULT = new RoleConfigType();

	public static class Holder extends ConfigHolder<Snowflake> {
		public Holder(GuildCollections gc, ConfigKey<Snowflake, Holder> key) {
			super(gc, key);
		}

		@Override
		public String toString() {
			return "<@&" + get().asString() + '>';
		}

		public boolean isSet() {
			return get().asLong() != 0L;
		}

		@Nullable
		public CachedRole getRole() {
			return isSet() ? gc.getRoleMap().get(get()) : null;
		}

		public Optional<CachedRole> role() {
			return isSet() ? Optional.ofNullable(getRole()) : Optional.empty();
		}

		public boolean is(Member member) {
			return isSet() && member.getRoleIds().contains(get());
		}

		public boolean isMentioned(Message message) {
			return isSet() && message.getRoleMentionIds().contains(get());
		}
	}

	@Override
	public String getTypeName() {
		return "role";
	}

	@Override
	public Holder createHolder(GuildCollections gc, ConfigKey<Snowflake, Holder> key) {
		return new Holder(gc, key);
	}

	@Override
	public String validate(GuildCollections guild, int type, String value) {
		return !value.isEmpty() && (!value.startsWith("@") && guild.getRoleMap().containsKey(Utils.snowflake(value)) || guild.getUniqueRoleNameMap().containsKey(value.substring(1))) ? "" : "Role not found!";
	}

	@Override
	public boolean hasEnumValues() {
		return true;
	}

	@Override
	public Collection<EnumValue> getEnumValues(GuildCollections guild) {
		var list = new ArrayList<EnumValue>();

		for (var entry : guild.getUniqueRoleNameMap().entrySet()) {
			list.add(new EnumValue(entry.getValue().id.asString(), "@" + entry.getKey()));
		}

		return list;
	}

	@Override
	public String serialize(GuildCollections guild, int type, Snowflake value) {
		for (var entry : guild.getUniqueRoleNameMap().entrySet()) {
			if (entry.getValue().id.equals(value)) {
				return "@" + entry.getKey();
			}
		}

		return "";
	}

	@Override
	public Snowflake deserialize(GuildCollections guild, int type, String value) {
		if (value.isEmpty()) {
			return Utils.NO_SNOWFLAKE;
		} else if (value.startsWith("@")) {
			var role = guild.getUniqueRoleNameMap().get(value.substring(1));
			return role == null ? Utils.NO_SNOWFLAKE : role.id;
		} else {
			return Utils.snowflake(value);
		}
	}
}
