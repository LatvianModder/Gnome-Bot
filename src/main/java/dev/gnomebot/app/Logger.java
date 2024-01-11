package dev.gnomebot.app;

import dev.latvian.apps.webutils.ansi.Ansi;
import dev.latvian.apps.webutils.ansi.AnsiCode;
import dev.latvian.apps.webutils.ansi.AnsiComponent;
import dev.latvian.apps.webutils.ansi.AnsiContext;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Logger {
	public static final ConcurrentLinkedDeque<AnsiComponent> BRAIN = new ConcurrentLinkedDeque<>();

	private final PrintStream out;
	private int event;

	public Logger() {
		out = System.out;
		event = 0;
	}

	public void replaceSystemOutLogger() {
		try {
			var fileOut = new PrintStream(new BufferedOutputStream(Files.newOutputStream(AppPaths.LOG)), true);
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

			if (event == 0) {
				Ansi.log("", false);
			}

			event++;

			var builder = new StringBuilder();
			component.appendAnsi(builder, AnsiContext.NONE);
			builder.append(' ');
			out.print(builder);

			if (event >= 40) {
				out.println();
				event = 0;
			}
		}
	}
}
