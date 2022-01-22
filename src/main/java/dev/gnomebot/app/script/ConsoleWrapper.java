package dev.gnomebot.app.script;

import dev.gnomebot.app.App;

public class ConsoleWrapper {
	public static void log(Object text) {
		App.info(text);
	}

	public static void info(Object text) {
		App.info(text);
	}

	public static void error(Object text) {
		App.error(text);
	}

	public static void warn(Object text) {
		App.warn(text);
	}
}
