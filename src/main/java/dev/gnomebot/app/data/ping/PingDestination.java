package dev.gnomebot.app.data.ping;

public interface PingDestination {
	PingDestination NONE = (data, ping) -> {
	};

	void relayPing(PingData data, Ping ping);
}
