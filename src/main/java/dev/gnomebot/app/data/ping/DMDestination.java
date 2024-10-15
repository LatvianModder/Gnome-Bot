package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.apps.ansi.log.Log;

import java.time.Duration;

public class DMDestination implements PingDestination {
	public static final DMDestination INSTANCE = new DMDestination();

	@Override
	public void relayPing(long targetId, PingData pingData, Ping ping, UserPingConfig config) {
		try {
			var app = pingData.gc().db.app;
			var dmChannel = DM.openId(app.discordHandler, targetId);
			Log.info("Ping for DM/" + app.discordHandler.getUserName(targetId).orElse("Unknown") + " from " + pingData.username() + " @ **" + pingData.gc() + "** in " + pingData.channel().getName() + ": " + pingData.content() + " (" + ping.pattern() + ")");

			var content = new StringBuilder();

			if (config.silent()) {
				content.append("@silent ");
			}

			content.append("[Ping ➤](").append(pingData.url()).append(") from ").append(pingData.url()).append("\n").append(pingData.content());

			app.discordHandler.client.getRestClient().getChannelService().createMessage(dmChannel, MessageBuilder.create(EmbedBuilder.create()
							.author(pingData.username(), pingData.avatar())
							.description(content.toString())
					).toMultipartMessageCreateRequest())
					.timeout(Duration.ofSeconds(5L))
					.block();
		} catch (Exception ex) {
			Log.error("Failed to DM user " + targetId + ": " + ex);
		}
	}

	@Override
	public String toString() {
		return "DM";
	}
}