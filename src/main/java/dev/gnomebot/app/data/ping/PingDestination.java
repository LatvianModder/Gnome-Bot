package dev.gnomebot.app.data.ping;

import discord4j.common.util.Snowflake;

public interface PingDestination {
	PingDestination NONE = (targetId, data, ping) -> {
	};

	void relayPing(Snowflake targetId, PingData data, Ping ping);
}
