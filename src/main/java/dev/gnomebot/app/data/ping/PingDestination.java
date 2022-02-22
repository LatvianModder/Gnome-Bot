package dev.gnomebot.app.data.ping;

public interface PingDestination {
	PingDestination NONE = data -> {
	};

	void relayPing(PingData data);
}
