package dev.gnomebot.app.discord;

import com.google.gson.JsonElement;
import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.Pair;
import dev.gnomebot.app.util.URLRequest;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScamHandler {
	private static final Object LOCK = new Object();

	public static Set<String> REMOTE_BAD_DOMAINS = Collections.emptySet();
	public static Map<String, Boolean> OVERRIDES = new HashMap<>();

	static {
		OVERRIDES.put("store.steampowered.com", false);
		OVERRIDES.put("steamcommunity.com", false);
		OVERRIDES.put("steamcharts.com", false);
		OVERRIDES.put("discord.com", false);
		OVERRIDES.put("discordapp.com", false);
		OVERRIDES.put("cdn.discordapp.com", false);
		OVERRIDES.put("media.discordapp.net", false);
		OVERRIDES.put("canary.discord.com", false);
	}

	public static final Pattern URL_SHORTENER_PATTERN = Pattern.compile("(?:adf\\.ly|bit\\.ly)/\\w+", Pattern.MULTILINE);
	public static final Pattern ONLY_SYMBOLS = Pattern.compile("[-0-9()@:%_+.~#?&/=]+");
	public static final Pattern STEAM_PATTERN = Pattern.compile("(st[\\w.]+\\.\\w{2,6}(?:\\.\\w{2,6})?)/_?(?:tr|\\?p|new|app|profile)", Pattern.MULTILINE);
	public static final Pattern NITRO_PATTERN = Pattern.compile("giveawaynitro|nitro-discord\\.\\w+|discord-nitro\\.\\w+|/(?:free|airdrop-)?nitro|turbodlscord|discord(?:nitro)?gift", Pattern.MULTILINE);
	public static final Pattern EXACT_DOMAIN = Pattern.compile("[\\w.-]+");

	public static void fetchBadDomains() {
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
			boolean save = false;

			if (Files.exists(AppPaths.FILES_BAD_DOMAINS)) {
				try {
					List<String> lines = Files.readAllLines(AppPaths.FILES_BAD_DOMAINS);
					REMOTE_BAD_DOMAINS = new HashSet<>(lines);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else {
				save = true;
			}

			if (Files.exists(AppPaths.FILES_BAD_DOMAIN_OVERRIDES)) {
				try {
					List<String> lines = Files.readAllLines(AppPaths.FILES_BAD_DOMAIN_OVERRIDES);
					OVERRIDES.clear();

					for (String s : lines) {
						String[] split = s.trim().split(": ", 2);

						if (split.length == 2 && !split[0].trim().isEmpty() && !split[1].trim().isEmpty()) {
							OVERRIDES.put(split[0].trim().toLowerCase(), Boolean.parseBoolean(split[1].trim()));
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else {
				save = true;
			}

			if (save) {
				saveBadDomainsSynced();
			}
		}
	}

	public static void saveBadDomains() {
		synchronized (LOCK) {
			saveBadDomainsSynced();
		}
	}

	private static void saveBadDomainsSynced() {
		try {
			Files.write(AppPaths.FILES_BAD_DOMAINS, REMOTE_BAD_DOMAINS);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			Files.write(AppPaths.FILES_BAD_DOMAIN_OVERRIDES, OVERRIDES.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).toList());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void setOverride(GuildCollections gc, String user, String domain, @Nullable Boolean override) {
		synchronized (LOCK) {
			App.error("Domain " + domain + " set to " + override + " by " + user + " in " + gc);

			if (override == null) {
				OVERRIDES.remove(domain.trim().toLowerCase());
			} else {
				OVERRIDES.put(domain.trim().toLowerCase(), override);
			}

			saveBadDomainsSynced();
		}
	}

	public static Boolean checkScamDomain(String domain) {
		synchronized (LOCK) {
			Boolean b = OVERRIDES.get(domain);

			if (b != null) {
				return b;
			} else if (REMOTE_BAD_DOMAINS.contains(domain)) {
				return true;
			} else if (ONLY_SYMBOLS.matcher(domain).matches()) {
				return false;
			}

			return null;
		}
	}

	// URL, Domain
	@Nullable
	public static Pair<String, String> checkScam(String content) {
		Matcher urlMatcher = MessageHandler.URL_PATTERN.matcher(content);

		while (urlMatcher.find()) {
			String url = urlMatcher.group();
			String domain = urlMatcher.group(1);

			if (domain.startsWith("www.")) {
				domain = domain.substring(4);
			}

			if (domain.isEmpty()) {
				continue;
			}

			Boolean override = checkScamDomain(domain);

			if (override != null) {
				if (override) {
					return Pair.of(url, domain);
				} else {
					continue;
				}
			}

			Matcher nitroScamMatcher = ScamHandler.NITRO_PATTERN.matcher(url);

			if (nitroScamMatcher.find()) {
				return Pair.of(nitroScamMatcher.group(), domain);
			}

			Matcher steamScamMatcher = ScamHandler.STEAM_PATTERN.matcher(url);

			if (steamScamMatcher.find()) {
				return Pair.of(steamScamMatcher.group(), domain);
			}
		}

		return null;
	}
}
