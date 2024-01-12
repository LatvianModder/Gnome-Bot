package dev.gnomebot.app.data.ping;

import java.util.Arrays;

public record PingDestinationBundle(PingDestination[] destinations) implements PingDestination {
	public static final PingDestination[] EMPTY_ARRAY = new PingDestination[0];

	@Override
	public void relayPing(long targetId, PingData data, Ping ping) {
		for (var destination : destinations) {
			destination.relayPing(targetId, data, ping);
		}
	}

	@Override
	public String toString() {
		return Arrays.toString(destinations);
	}
}
