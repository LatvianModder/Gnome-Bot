package dev.gnomebot.app.data.ping;

public interface PingDestination {
	PingDestination NONE = (targetId, data, ping) -> {
	};

	void relayPing(long targetId, PingData data, Ping ping);
}
