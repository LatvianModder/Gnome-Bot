package dev.gnomebot.app.data.ping;

import dev.latvian.apps.ansi.log.Log;

public record RelayPingTask(
		PingDestination destination,
		long targetId,
		PingData pingData,
		Ping ping,
		UserPingConfig config
) implements Runnable {
	@Override
	public void run() {
		try {
			var start = System.nanoTime();

			if (targetId == 0L || pingData.channel().canViewChannel(targetId)) {
				destination.relayPing(targetId, pingData, ping, config);
				// Log.debug(targetId + " pinged in " + pingData.channel() + " by " + pingData.username() + " with " + pingData.match() + " (" + ping.pattern() + ")");
			} /* else {
				Log.warn(targetId + " doesn't have permission to view channel " + pingData.channel() + " but got ping from " + pingData.username() + ": " + pingData.match() + " (" + ping.pattern() + ")");
			} */

			var time = System.nanoTime() - start;

			if (time >= 1_000_000_000L) { // 1000.000 ms
				Log.warn("Reply: " + ((time / 1000L) / 1000F) + " ms " + destination);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
