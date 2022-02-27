package dev.gnomebot.app.data.ping;

import discord4j.common.util.Snowflake;

public record UserPingDestination(Snowflake userId, PingDestination dst) implements PingDestination {
	@Override
	public void relayPing(PingData pingData) {
		if (pingData.channel().canViewChannel(userId)) {
			dst.relayPing(pingData);
		}
	}

	@Override
	public String toString() {
		return dst.toString();
	}
}