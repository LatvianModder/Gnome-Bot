package dev.gnomebot.app;

import dev.latvian.apps.webutils.ansi.Ansi;
import dev.latvian.apps.webutils.ansi.AnsiCode;
import dev.latvian.apps.webutils.ansi.AnsiComponent;
import discord4j.core.object.presence.ClientPresence;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Logger {
	public static final ConcurrentLinkedDeque<AnsiComponent> BRAIN = new ConcurrentLinkedDeque<>();

	private final PrintStream out;
	private int event;
	private boolean firstEvent;

	public Logger() {
		out = System.out;
		event = 0;
		firstEvent = true;
	}

	public void replaceSystemOutLogger() {
		try {
			PrintStream fileOut = new PrintStream(new BufferedOutputStream(Files.newOutputStream(AppPaths.LOG)), true);
			System.setOut(fileOut);
			System.setErr(fileOut);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void log(Object message) {
		synchronized (BRAIN) {
			if (event > 0) {
				// out.println(Ansi.RESET);
				System.out.println(AnsiCode.RESET);
				event = 0;
			}

			// out.println(Ansi.CYAN + FORMATTER.format(new Date()) + color + " " + message + Ansi.RESET);
			Ansi.log(message);
		}
	}

	public void info(Object message) {
		log(message);
	}

	public void success(Object message) {
		log(Ansi.green(message));
	}

	public void warn(Object message) {
		log(Ansi.orange(message));
	}

	public void error(Object message) {
		log(Ansi.of(message).error());
	}

	public void event(AnsiComponent component) {
		synchronized (BRAIN) {
			if (BRAIN.size() > 750) {
				BRAIN.removeFirst();
			}

			BRAIN.add(component);

			if (event == 0 || event >= 40) {
				Ansi.log("");
				event = 0;
			}

			event++;
			out.print(component.toString() + " ");

			if (firstEvent) {
				App.instance.discordHandler.client.updatePresence(ClientPresence.online()).subscribe();
				firstEvent = false;
			}
		}
	}
}
