package dev.gnomebot.app.data.ping;

import discord4j.common.util.Snowflake;

import java.util.Arrays;

public record UserPingInstance(Ping[] pings, Snowflake user, PingDestination destination, UserPingConfig config) {
	public void handle(PingData pingData) {
		if ((config.self() || pingData.userId().asLong() != user.asLong()) && config.match(pingData) && match(pingData.content())) {
			destination.relayPing(pingData);
		}
	}

	public boolean match(String content) {
		for (Ping ping : pings) {
			if (ping.pattern().matcher(content).find()) {
				return ping.allow();
			}
		}

		return false;
	}

	@Override
	public String toString() {
		return "{pings=" + Arrays.toString(pings) +
				", user=" + user.asString() +
				", destination=" + destination +
				", config=" + config +
				'}';
	}
}
