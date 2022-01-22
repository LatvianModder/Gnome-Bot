package dev.gnomebot.app;

import dev.gnomebot.app.util.Ansi;
import discord4j.core.object.presence.ClientPresence;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class Logger {
	public static final SimpleDateFormat FORMATTER = new SimpleDateFormat("HH:mm:ss");

	public static class SysOut extends PrintStream {
		public final String color;

		public SysOut(PrintStream out, String c) {
			super(out);
			color = c;
		}

		// TODO: Wrap output methods with color codes
	}

	private final PrintStream out = System.out;
	private final Object eventLock = new Object();
	private final AtomicInteger event = new AtomicInteger(0);
	private boolean firstEvent = true;

	public Logger() {
		System.setOut(new SysOut(out, Ansi.RESET));
		System.setErr(new SysOut(out, Ansi.DARK_RED));
	}

	public void log(Object message, String color) {
		synchronized (eventLock) {
			if (event.get() > 0) {
				out.println(Ansi.RESET);
				event.set(0);
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

	public void event(String string) {
		synchronized (eventLock) {
			if (event.get() == 0) {
				out.print(Ansi.CYAN + FORMATTER.format(new Date()) + Ansi.RESET + " ");
			}

			int e = event.incrementAndGet();
			out.print(string);

			if (e >= 40) {
				out.println(Ansi.RESET);
				event.set(0);
			}

			if (firstEvent) {
				App.instance.discordHandler.client.updatePresence(ClientPresence.online()).subscribe();
				firstEvent = false;
			}
		}
	}

	public void messageCreatedNoRole() {
		event(Ansi.LIGHT_GRAY + "■ " + Ansi.RESET);
	}

	public void messageCreatedAnyRole() {
		event(Ansi.YELLOW + "■ " + Ansi.RESET);
	}

	public void unknownMessage() {
		event(Ansi.TEAL + "■ " + Ansi.RESET);
	}

	public void messageCreatedAdmin() {
		event(Ansi.PURPLE + "■ " + Ansi.RESET);
	}

	public void messageCreatedBot() {
		event(Ansi.GREEN + "■ " + Ansi.RESET);
	}

	public void messageEdited() {
		event(Ansi.ORANGE + "■ " + Ansi.RESET);
	}

	public void messageDeleted() {
		event(Ansi.RED + "■ " + Ansi.RESET);
	}

	public void suspiciousMessage() {
		event(Ansi.DARK_RED + "■ " + Ansi.RESET);
	}

	public void commandSuccess() {
		event(Ansi.BLUE + "◆ " + Ansi.RESET);
	}

	public void commandFail() {
		event(Ansi.RED + "◆ " + Ansi.RESET);
	}

	public void reactionAdded() {
		event(Ansi.GREEN + "\uD83D\uDDF8 " + Ansi.RESET); // 🗸
	}

	public void reactionRemoved() {
		event(Ansi.RED + "\uD83D\uDDF8 " + Ansi.RESET); // 🗸
	}

	public void voiceJoined() {
		event(Ansi.GREEN + "♪ " + Ansi.RESET);
	}

	public void voiceLeft() {
		event(Ansi.RED + "♪ " + Ansi.RESET);
	}

	public void voiceChanged() {
		event(Ansi.YELLOW + "♪ " + Ansi.RESET);
	}

	public void refreshedGuildCache() {
		event(Ansi.LIGHT_GRAY + "\uD83D\uDE7E " + Ansi.RESET); // 🙾
	}

	public void refreshedChannelCache() {
		event(Ansi.MAGENTA + "\uD83D\uDE7E " + Ansi.RESET); // 🙾
	}

	public void refreshedMemberCache() {
		event(Ansi.GREEN + "\uD83D\uDE7E " + Ansi.RESET); // 🙾
	}

	public void refreshedRoleCache() {
		event(Ansi.YELLOW + "\uD83D\uDE7E " + Ansi.RESET); // 🙾
	}

	public void memberJoined() {
		event(Ansi.BLUE + "⬤ " + Ansi.RESET);
	}

	public void memberLeft() {
		event(Ansi.RED + "⬤ " + Ansi.RESET);
	}

	public void memberMuted() {
		event(Ansi.RED + "☠ " + Ansi.RESET);
	}

	public void memberBanned() {
		event(Ansi.DARK_RED + "☠ " + Ansi.RESET);
	}

	public void webRequest() {
		event(Ansi.CYAN + "◆ " + Ansi.RESET);
	}

	public void presenceUpdated() {
		event(Ansi.LIGHT_GRAY + "◆ " + Ansi.RESET);
	}
}
