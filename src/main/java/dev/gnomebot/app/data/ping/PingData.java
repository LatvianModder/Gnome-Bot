package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.channel.ChannelInfo;
import discord4j.core.object.entity.User;

public record PingData(GuildCollections gc,
					   ChannelInfo channel,
					   User user,
					   long userId,
					   String username,
					   String avatar,
					   boolean bot,
					   String match,
					   String content,
					   String url
) {
}
