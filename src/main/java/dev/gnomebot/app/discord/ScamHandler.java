package dev.gnomebot.app.discord;

import com.google.gson.JsonElement;
import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.util.URLRequest;

import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ScamHandler {
	private static final Object LOCK = new Object();

	public static Set<String> REMOTE_BAD_DOMAINS = Collections.emptySet();

	public static final Pattern URL_SHORTENER_PATTERN = Pattern.compile("(?:adf\\.ly|bit\\.ly)/\\w+", Pattern.MULTILINE);
	// https://steambutactuallygnometest.com/tradeoffer/new/test
	public static final Pattern STEAM_PATTERN = Pattern.compile("(st[\\w.]+\\.\\w{2,6}(?:\\.\\w{2,6})?)/_?(?:tr|\\?p|new|app|profile)", Pattern.MULTILINE);
	public static final Pattern NITRO_PATTERN = Pattern.compile("giveawaynitro|nitro-discord\\.\\w+|discord-nitro\\.\\w+|/(?:free|airdrop-)?nitro|turbodlscord|discord(?:nitro)?gift|discorcl.click|steamnitro|gave-nitro|discord-airdrop|discord-give|take-nitro|discocrd\\.gift|dlscord-app|steamdlscords|dlscord|dlcsorcl|discordt|discordc|discordd|dizcord|discorte|discord-true|discrods|discorc|discrds", Pattern.MULTILINE);

	public static void updateRemoteBadDomains() {
		URLRequest.of("https://phish.sinking.yachts/v2/all").addHeader("X-Identity", "gnomebot.dev+" + App.instance.discordHandler.selfId.asString()).toJsonArray().subscribe(callback -> {
			if (callback.isLeft()) {
				Set<String> s = new HashSet<>();

				for (JsonElement e : callback.getLeft()) {
					s.add(e.getAsString());
				}

				synchronized (LOCK) {
					REMOTE_BAD_DOMAINS = s;
				}

				saveBadDomains();
			}
		});
	}

	public static void loadBadDomains() {
		synchronized (LOCK) {
			if (Files.exists(AppPaths.FILES.resolve("bad_domains.txt"))) {
				try {
					REMOTE_BAD_DOMAINS = new HashSet<>(Files.readAllLines(AppPaths.FILES.resolve("bad_domains.txt")));
				} catch (Exception ex) {
				}
			}
		}
	}

	public static void saveBadDomains() {
		synchronized (LOCK) {
			try {
				Files.write(AppPaths.FILES.resolve("bad_domains.txt"), REMOTE_BAD_DOMAINS);
			} catch (Exception ex) {
			}
		}
	}

	public static boolean check(String domain) {
		synchronized (LOCK) {
			return REMOTE_BAD_DOMAINS.contains(domain);
		}
	}
}
