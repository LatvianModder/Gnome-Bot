package dev.gnomebot.app.data.ping;

public interface PingDestination {
	PingDestination NONE = (targetId, data, ping, config) -> {
	};

	void relayPing(long targetId, PingData data, Ping ping, UserPingConfig config);
}
