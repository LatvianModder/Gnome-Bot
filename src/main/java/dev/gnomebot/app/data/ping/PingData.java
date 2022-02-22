package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.GuildCollections;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;

public record PingData(GuildCollections gc, ChannelInfo channel, User user, Snowflake userId, String username, String avatar, boolean bot, String content, String url) {
}
