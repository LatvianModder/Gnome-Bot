package dev.gnomebot.app.script;

import dev.latvian.apps.ansi.log.Log;

public class ConsoleWrapper {
	public static void log(Object text) {
		Log.info(text);
	}

	public static void info(Object text) {
		Log.info(text);
	}

	public static void error(Object text) {
		Log.error(text);
	}

	public static void warn(Object text) {
		Log.warn(text);
	}
}
