package dev.gnomebot.app.data.config;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.GuildCollections;
import discord4j.common.util.Snowflake;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChannelConfigKey extends SnowflakeConfigKey {
	public ChannelConfigKey(GuildCollections gc, String name) {
		super(gc, name);
		enumValues(this::getEnumValues);
	}

	private List<EnumValue> getEnumValues() {
		var list = new ArrayList<EnumValue>();

		for (var c : gc.getChannelList()) {
			list.add(new EnumValue(c.id.asString(), "#" + c.getName()));
		}

		return list;
	}

	@Override
	public String getType() {
		return "channel";
	}

	@Override
	public String toString() {
		return "<#" + get().asString() + '>';
	}

	public Optional<ChannelInfo> messageChannel() {
		return isSet() ? Optional.ofNullable(gc.getChannelMap().get(get())) : Optional.empty();
	}

	public boolean is(Snowflake id) {
		return isSet() && get().equals(id);
	}
}
