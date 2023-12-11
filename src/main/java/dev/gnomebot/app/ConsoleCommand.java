package dev.gnomebot.app;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsoleCommand {
	public final Pattern pattern;
	public final Consumer<Matcher> callback;

	public ConsoleCommand(Pattern p, Consumer<Matcher> e) {
		pattern = p;
		callback = e;
	}
}