package dev.gnomebot.app;

import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.ansi.log.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WatchdogThread extends Thread {
	public static final int MAX_ERRORS = 3;

	public final App app;
	public static long lastUpdate;
	private static int majorErrors = 0;

	public static List<String> errorList = new ArrayList<>();

	public WatchdogThread(App a) {
		super("Watchdog");
		setDaemon(true);
		app = a;
		errorList.add("RIP");
	}

	public static void update() {
		lastUpdate = System.currentTimeMillis();
	}

	@SuppressWarnings("deprecation")
	public static void majorError(String reason) {
		if (majorErrors < MAX_ERRORS) {
			majorErrors++;
			Log.error("Watchdog: Major error occurred [" + majorErrors + "/" + MAX_ERRORS + "]: " + reason);
			errorList.add(new Date().toLocaleString() + ": " + reason);
		}
	}

	@Override
	public void run() {
		update();

		if (true) {
			return;
		}

		while (app.running) {
			if ((System.currentTimeMillis() - lastUpdate) >= 60000L * MAX_ERRORS) {
				if (app.blockingTasks.isEmpty()) {
					try {
						// TODO: Remove hardcoded
						app.db.guild(SnowFlake.convert(720671115336220693L)).getChannelInfo(849420356237656074L).createMessage("SOS!").block().delete().block();
						update();
					} catch (Exception ex) {
						ex.printStackTrace();
						Log.error("Watchdog: Timed out");
						majorErrors = MAX_ERRORS;
					}
				} else {
					Log.error("Watchdog: Blocking task is taking more than 1 minute! Tasks:");
					Log.error(app.blockingTasks);
				}
			}

			if (majorErrors >= MAX_ERRORS) {
				majorErrors = MAX_ERRORS - 1;
				majorError("Timed out");
				Config.get().death_webhook.execute(MessageBuilder.create().content(errorList));
				app.restart();
				break;
			}
		}

		try {
			Thread.sleep(5000L);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		app.restart();
	}
}
