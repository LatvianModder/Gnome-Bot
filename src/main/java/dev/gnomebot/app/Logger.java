package dev.gnomebot.app;

import dev.gnomebot.app.util.Ansi;
import discord4j.core.object.presence.ClientPresence;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class Logger {
	public static final SimpleDateFormat FORMATTER = new SimpleDateFormat("HH:mm:ss");
	public static final String[] BRAIN = new String[750];

	static {
		Arrays.fill(BRAIN, "");
	}

	private final PrintStream out;
	private final Object eventLock;
	private int event;
	private boolean firstEvent;

	public Logger() {
		out = System.out;
		eventLock = new Object();
		event = 0;
		firstEvent = true;

		try {
			PrintStream fileOut = new PrintStream(new BufferedOutputStream(Files.newOutputStream(AppPaths.LOG)), true);
			System.setOut(fileOut);
			System.setErr(fileOut);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void log(Object message, String color) {
		synchronized (eventLock) {
			if (event > 0) {
				out.println(Ansi.RESET);
				event = 0;
			}

			out.println(Ansi.CYAN + FORMATTER.format(new Date()) + color + " " + message + Ansi.RESET);
		}
	}

	public void info(Object message) {
		log(message, Ansi.RESET);
	}

	public void success(Object message) {
		log(message, Ansi.GREEN);
	}

	public void warn(Object message) {
		log(message, Ansi.ORANGE);
	}

	public void error(Object message) {
		log(message, Ansi.DARK_RED);
	}

	public void event(String color, String character) {
		synchronized (eventLock) {
			System.arraycopy(BRAIN, 0, BRAIN, 1, BRAIN.length - 1);
			BRAIN[0] = color + character;

			if (event == 0) {
				out.print(Ansi.CYAN + FORMATTER.format(new Date()) + Ansi.RESET + " ");
			}

			event++;
			out.print(color + character + " " + Ansi.RESET);

			if (event >= 40) {
				out.println(Ansi.RESET);
				event = 0;
			}

			if (firstEvent) {
				App.instance.discordHandler.client.updatePresence(ClientPresence.online()).subscribe();
				firstEvent = false;
			}
		}
	}

	public void messageCreatedNoRole() {
		event(Ansi.LIGHT_GRAY, "■");
	}

	public void messageCreatedAnyRole() {
		event(Ansi.YELLOW, "■");
	}

	public void unknownMessage() {
		event(Ansi.TEAL, "■");
	}

	public void messageCreatedAdmin() {
		event(Ansi.PURPLE, "■");
	}

	public void messageCreatedBot() {
		event(Ansi.GREEN, "■");
	}

	public void messageEdited() {
		event(Ansi.ORANGE, "■");
	}

	public void messageDeleted() {
		event(Ansi.RED, "■");
	}

	public void suspiciousMessage() {
		event(Ansi.DARK_RED, "■");
	}

	public void commandSuccess() {
		event(Ansi.BLUE, "◆");
	}

	public void commandFail() {
		event(Ansi.RED, "◆");
	}

	public void reactionAdded() {
		event(Ansi.GREEN, "\uD83D\uDDF8"); // 🗸
	}

	public void reactionRemoved() {
		event(Ansi.RED, "\uD83D\uDDF8"); // 🗸
	}

	public void voiceJoined() {
		event(Ansi.GREEN, "♪");
	}

	public void voiceLeft() {
		event(Ansi.RED, "♪");
	}

	public void voiceChanged() {
		event(Ansi.YELLOW, "♪");
	}

	public void refreshedGuildCache() {
		event(Ansi.LIGHT_GRAY, "\uD83D\uDE7E"); // 🙾
	}

	public void refreshedChannelCache() {
		event(Ansi.MAGENTA, "\uD83D\uDE7E"); // 🙾
	}

	public void refreshedPings() {
		event(Ansi.GREEN, "\uD83D\uDE7E"); // 🙾
	}

	public void refreshedRoleCache() {
		event(Ansi.YELLOW, "\uD83D\uDE7E"); // 🙾
	}

	public void memberJoined() {
		event(Ansi.BLUE, "⬤");
	}

	public void memberLeft() {
		event(Ansi.RED, "⬤");
	}

	public void memberMuted() {
		event(Ansi.RED, "☠");
	}

	public void memberBanned() {
		event(Ansi.DARK_RED, "☠");
	}

	public void webRequest() {
		event(Ansi.CYAN, "◆");
	}

	public void presenceUpdated() {
		event(Ansi.LIGHT_GRAY, "◆");
	}
}
