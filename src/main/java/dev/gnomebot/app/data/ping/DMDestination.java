package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.PrivateChannel;

public record DMDestination(Snowflake userId, PrivateChannel dmChannel) implements PingDestination {
	@Override
	public void relayPing(PingData pingData) {
		try {
			dmChannel.createMessage(MessageBuilder.create(EmbedBuilder.create()
					.author(pingData.username() + " [" + pingData.gc() + "]", pingData.avatar())
					.description("[➤](" + pingData.url() + ") " + pingData.content())
			).toMessageCreateSpec()).block();
		} catch (Exception ex) {
		}
	}

	@Override
	public String toString() {
		return "DM{" + userId.asString() + "}";
	}
}