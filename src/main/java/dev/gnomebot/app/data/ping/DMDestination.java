package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;

import java.time.Duration;

public class DMDestination implements PingDestination {
	public static final DMDestination INSTANCE = new DMDestination();

	@Override
	public void relayPing(long targetId, PingData pingData, Ping ping) {
		try {
			var app = pingData.gc().db.app;
			var dmChannel = DM.openId(app.discordHandler, targetId);
			App.info("Ping for DM/" + app.discordHandler.getUserName(targetId).orElse("Unknown") + " from " + pingData.username() + " @ **" + pingData.gc() + "** in " + pingData.channel().getName() + ": " + pingData.content() + " (" + ping.pattern() + ")");

			app.discordHandler.client.getRestClient().getChannelService().createMessage(dmChannel, MessageBuilder.create(EmbedBuilder.create()
							.author(pingData.username(), pingData.avatar())
							.description("[Ping ➤](" + pingData.url() + ") from " + pingData.url() + "\n" + pingData.content())
					).toMultipartMessageCreateRequest())
					.timeout(Duration.ofSeconds(5L))
					.block();
		} catch (Exception ex) {
			App.error("Failed to DM user " + targetId + ": " + ex);
		}
	}

	@Override
	public String toString() {
		return "DM";
	}
}