package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.util.TimeLimitedCharSequence;
import dev.latvian.apps.ansi.log.Log;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public record UserPingInstance(Ping[] pings, long user, PingDestination destination, UserPingConfig config) {
	public void handle(PingData pingData) {
		if ((config.self() || pingData.userId() != user) && config.match(pingData)) {
			var start = System.nanoTime();
			var ping = match(pingData.content());

			if (ping != null) {
				var time = System.nanoTime() - start;

				if (time >= 100_000L) { // 0.100 ms
					Log.warn("Match: " + ((time / 1000L) / 1000F) + " ms " + this);
				}

				CompletableFuture.runAsync(new RelayPingTask(destination, user, pingData, ping, config));
			}
		}
	}

	@Nullable
	public Ping test(PingData pingData) {
		if ((config.self() || pingData.userId() != user) && config.match(pingData)) {
			var start = System.nanoTime();
			var ping = match(pingData.content());

			if (ping != null) {
				var time = System.nanoTime() - start;

				if (time >= 100_000L) { // 0.100 ms
					Log.warn("Match: " + ((time / 1000L) / 1000F) + " ms " + this);
				}

				return ping;
			}
		}

		return null;
	}

	public Ping match(String content) {
		try {
			for (var ping : pings) {
				if (ping != null && ping.pattern().matcher(new TimeLimitedCharSequence(content, 100L)).find()) {
					return ping.allow() ? ping : null;
				}
			}
		} catch (Exception ex) {
			return null;
		}

		return null;
	}

	@Override
	public String toString() {
		return "{pings=" + Arrays.toString(pings) +
				", user=" + user +
				", destination=" + destination +
				", config=" + config +
				'}';
	}
}
