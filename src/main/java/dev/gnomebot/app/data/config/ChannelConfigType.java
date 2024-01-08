package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class ChannelConfigType implements SnowflakeConfigType<ChannelConfigType.Holder> {
	public static ChannelConfigType DEFAULT = new ChannelConfigType();

	public static class Holder extends ConfigHolder<Snowflake> {
		public Holder(GuildCollections gc, ConfigKey<Snowflake, Holder> key) {
			super(gc, key);
		}

		@Override
		public String toString() {
			return "<#" + get().asString() + '>';
		}

		public boolean isSet() {
			return get().asLong() != 0L;
		}

		@Nullable
		public ChannelInfo getMessageChannel() {
			return isSet() ? gc.getChannelMap().get(get()) : null;
		}

		public Optional<ChannelInfo> messageChannel() {
			return isSet() ? Optional.ofNullable(getMessageChannel()) : Optional.empty();
		}

		public boolean is(Snowflake id) {
			return isSet() && get().equals(id);
		}
	}

	@Override
	public String getTypeName() {
		return "channel";
	}

	@Override
	public Holder createHolder(GuildCollections gc, ConfigKey<Snowflake, Holder> key) {
		return new Holder(gc, key);
	}

	@Override
	public String validate(GuildCollections guild, int type, String value) {
		return !value.isEmpty() && (!value.startsWith("#") && guild.getRoleMap().containsKey(Utils.snowflake(value)) || guild.getUniqueChannelNameMap().containsKey(value.substring(1))) ? "" : "Channel not found!";
	}

	@Override
	public boolean hasEnumValues() {
		return true;
	}

	@Override
	public Collection<EnumValue> getEnumValues(GuildCollections guild) {
		var list = new ArrayList<EnumValue>();

		for (var entry : guild.getUniqueChannelNameMap().entrySet()) {
			list.add(new EnumValue(entry.getValue().id.asString(), "#" + entry.getKey()));
		}

		return list;
	}

	@Override
	public String serialize(GuildCollections guild, int type, Snowflake value) {
		for (var entry : guild.getUniqueChannelNameMap().entrySet()) {
			if (entry.getValue().id.equals(value)) {
				return "#" + entry.getKey();
			}
		}

		return "";
	}

	@Override
	public Snowflake deserialize(GuildCollections guild, int type, String value) {
		if (value.isEmpty()) {
			return Utils.NO_SNOWFLAKE;
		} else if (value.startsWith("#")) {
			var channel = guild.getUniqueChannelNameMap().get(value.substring(1));
			return channel == null ? Utils.NO_SNOWFLAKE : channel.id;
		} else {
			return Utils.snowflake(value);
		}
	}
}
