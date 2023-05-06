package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.App;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.common.util.Snowflake;

import java.time.Duration;

public record DMDestination(Snowflake userId, long dmChannel) implements PingDestination {
	@Override
	public void relayPing(PingData pingData, Ping ping) {
		try {
			App.info("Ping for DM/" + App.instance.discordHandler.getUserName(userId).orElse("Unknown") + " from " + pingData.username() + " @ **" + pingData.gc() + "** in " + pingData.channel().getName() + ": " + pingData.content() + " (" + ping.pattern() + ")");

			App.instance.discordHandler.client.getRestClient().getChannelService().createMessage(dmChannel, MessageBuilder.create(EmbedBuilder.create()
							.author(pingData.username(), pingData.avatar())
							.description("[Ping ➤](" + pingData.url() + ") from " + pingData.url() + "\n" + pingData.content())
					).toMultipartMessageCreateRequest())
					.timeout(Duration.ofSeconds(5L))
					.block();
		} catch (Exception ex) {
			App.error("Failed to DM user " + userId + ": " + ex);
		}
	}

	@Override
	public String toString() {
		return "DM{" + userId.asString() + "}";
	}
}