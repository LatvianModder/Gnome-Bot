package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.channel.TopLevelChannelInfo;
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
		public TopLevelChannelInfo getMessageChannel() {
			return isSet() ? gc.channels().get(get()) : null;
		}

		public Optional<TopLevelChannelInfo> messageChannel() {
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
		return !value.isEmpty() && (!value.startsWith("#") && guild.channels().get(SnowFlake.num(value)) != null || guild.channels().uniqueNameMap.containsKey(value.substring(1))) ? "" : "Channel not found!";
	}

	@Override
	public boolean hasEnumValues() {
		return true;
	}

	@Override
	public Collection<EnumValue> getEnumValues(GuildCollections guild) {
		var list = new ArrayList<EnumValue>();

		for (var entry : guild.channels().uniqueNameMap.entrySet()) {
			list.add(new EnumValue(SnowFlake.str(entry.getValue().id), "#" + entry.getKey()));
		}

		return list;
	}

	@Override
	public String serialize(GuildCollections guild, int type, Long value) {
		for (var entry : guild.channels().uniqueNameMap.entrySet()) {
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
			var channel = guild.channels().uniqueNameMap.get(value.substring(1));
			return channel == null ? 0L : channel.id;
		} else {
			return SnowFlake.num(value);
		}
	}
}
