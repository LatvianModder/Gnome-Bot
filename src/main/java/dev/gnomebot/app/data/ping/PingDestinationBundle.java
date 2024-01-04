package dev.gnomebot.app.data.ping;

import discord4j.common.util.Snowflake;

import java.util.Arrays;

public record PingDestinationBundle(PingDestination[] destinations) implements PingDestination {
	public static final PingDestination[] EMPTY_ARRAY = new PingDestination[0];

	@Override
	public void relayPing(Snowflake targetId, PingData data, Ping ping) {
		for (PingDestination destination : destinations) {
			destination.relayPing(targetId, data, ping);
		}
	}

	@Override
	public String toString() {
		return Arrays.toString(destinations);
	}
}
