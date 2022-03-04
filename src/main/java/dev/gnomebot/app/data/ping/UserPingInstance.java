package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.App;
import dev.gnomebot.app.util.TimeLimitedCharSequence;
import discord4j.common.util.Snowflake;

import java.util.Arrays;

public record UserPingInstance(Ping[] pings, Snowflake user, PingDestination destination, UserPingConfig config) {
	public static class ThreadRelayPing extends Thread {
		private final PingDestination destination;
		private final PingData pingData;

		public ThreadRelayPing(PingDestination destination, PingData pingData) {
			this.destination = destination;
			this.pingData = pingData;
			setDaemon(true);
		}

		@Override
		public void run() {
			try {
				long start = System.nanoTime();
				destination.relayPing(pingData);
				long time = System.nanoTime() - start;

				if (time >= 1_000_000_000L) { // 1000.000 ms
					App.warn("Reply: " + ((time / 1000L) / 1000F) + " ms " + destination);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void handle(PingData pingData) {
		if ((config.self() || pingData.userId().asLong() != user.asLong()) && config.match(pingData)) {
			long start = System.nanoTime();

			if (match(pingData.match())) {
				long time = System.nanoTime() - start;

				if (time >= 100_000L) { // 0.100 ms
					App.warn("Match: " + ((time / 1000L) / 1000F) + " ms " + this);
				}

				new ThreadRelayPing(destination, pingData).start();
			}
		}
	}

	public boolean match(String content) {
		try {
			for (Ping ping : pings) {
				if (ping != null && ping.pattern().matcher(new TimeLimitedCharSequence(content, 100L)).find()) {
					return ping.allow();
				}
			}
		} catch (Exception ex) {
			return false;
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
