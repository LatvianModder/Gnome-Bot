package dev.gnomebot.app;

import dev.gnomebot.app.util.Ansi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class ConsoleCommandManager {
	private final App app;
	private final List<ConsoleCommand> commands = new ArrayList<>();

	public ConsoleCommandManager(App m) {
		app = m;
	}

	public void add(Pattern pattern, Consumer<Matcher> callback) {
		commands.add(new ConsoleCommand(pattern, callback));
	}

	public void add(String literal, Consumer<Matcher> callback) {
		add(Pattern.compile(literal, Pattern.LITERAL), callback);
	}

	public void startThread() {
		Thread consoleReader = new Thread(() -> {
			while (app.running) {
				try {
					processCommand(System.console().readLine().trim());
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}, "Console");

		consoleReader.setDaemon(true);
		consoleReader.start();
	}

	private void processCommand(String line) {
		for (ConsoleCommand command : commands) {
			Matcher matcher = command.pattern.matcher(line);

			if (matcher.find()) {
				command.callback.accept(matcher);
				return;
			}
		}

		App.error("Unknown command: " + Ansi.DARK_RED + line);
	}
}
