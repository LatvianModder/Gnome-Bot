package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.SnowFlake;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class ChannelConfigType implements SnowflakeConfigType<ChannelConfigType.Holder> {
	public static ChannelConfigType DEFAULT = new ChannelConfigType();

	public static class Holder extends ConfigHolder<Long> {
		public Holder(GuildCollections gc, ConfigKey<Long, Holder> key) {
			super(gc, key);
		}

		@Override
		public String toString() {
			return "<#" + get() + '>';
		}

		public boolean isSet() {
			return get() != 0L;
		}

		@Nullable
		public ChannelInfo getMessageChannel() {
			return isSet() ? gc.getChannelMap().get(get()) : null;
		}

		public Optional<ChannelInfo> messageChannel() {
			return isSet() ? Optional.ofNullable(getMessageChannel()) : Optional.empty();
		}

		public boolean is(long id) {
			return isSet() && get() == id;
		}
	}

	@Override
	public String getTypeName() {
		return "channel";
	}

	@Override
	public Holder createHolder(GuildCollections gc, ConfigKey<Long, Holder> key) {
		return new Holder(gc, key);
	}

	@Override
	public String validate(GuildCollections guild, int type, String value) {
		return !value.isEmpty() && (!value.startsWith("#") && guild.getRoleMap().containsKey(SnowFlake.num(value)) || guild.getUniqueChannelNameMap().containsKey(value.substring(1))) ? "" : "Channel not found!";
	}

	@Override
	public boolean hasEnumValues() {
		return true;
	}

	@Override
	public Collection<EnumValue> getEnumValues(GuildCollections guild) {
		var list = new ArrayList<EnumValue>();

		for (var entry : guild.getUniqueChannelNameMap().entrySet()) {
			list.add(new EnumValue(SnowFlake.str(entry.getValue().id), "#" + entry.getKey()));
		}

		return list;
	}

	@Override
	public String serialize(GuildCollections guild, int type, Long value) {
		for (var entry : guild.getUniqueChannelNameMap().entrySet()) {
			if (entry.getValue().id == value) {
				return "#" + entry.getKey();
			}
		}

		return "";
	}

	@Override
	public Long deserialize(GuildCollections guild, int type, String value) {
		if (value.isEmpty()) {
			return 0L;
		} else if (value.startsWith("#")) {
			var channel = guild.getUniqueChannelNameMap().get(value.substring(1));
			return channel == null ? 0L : channel.id;
		} else {
			return SnowFlake.num(value);
		}
	}
}
