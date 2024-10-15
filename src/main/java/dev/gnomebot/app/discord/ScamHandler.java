package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.URLRequest;
import dev.latvian.apps.ansi.log.Log;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ScamHandler {
	public enum Type {
		DEFAULT("default", "No Override"),
		BLOCK("block", "Blocked"),
		ALLOW("allow", "Allowed");

		public final String id;
		public final String commandName;

		Type(String id, String commandName) {
			this.id = id;
			this.commandName = commandName;
		}

		@Override
		public String toString() {
			return id;
		}

		public static final Map<String, Type> MAP = Arrays.stream(values()).collect(Collectors.toMap(Type::toString, Function.identity()));
	}

	public record Scam(String url, String domain, String type) {
		@Override
		public String toString() {
			return type + "/" + domain + " (" + url + ")";
		}
	}

	private static final Object LOCK = new Object();

	public static Instant lastRemoteUpdate = null;
	public static Set<String> REMOTE_BAD_DOMAINS = Collections.emptySet();
	public static Map<String, Type> OVERRIDES = new HashMap<>();

	static {
		OVERRIDES.put("store.steampowered.com", Type.ALLOW);
		OVERRIDES.put("steamcommunity.com", Type.ALLOW);
		OVERRIDES.put("steamcharts.com", Type.ALLOW);
		OVERRIDES.put("discord.com", Type.ALLOW);
		OVERRIDES.put("discordapp.com", Type.ALLOW);
		OVERRIDES.put("cdn.discordapp.com", Type.ALLOW);
		OVERRIDES.put("media.discordapp.net", Type.ALLOW);
		OVERRIDES.put("canary.discord.com", Type.ALLOW);
		OVERRIDES.put("gist.github.com", Type.ALLOW);
	}

	public static Set<String> URL_SHORTENERS = new HashSet<>();

	static {
		URL_SHORTENERS.add("tinyurl.com");
		URL_SHORTENERS.add("bit.ly");
		URL_SHORTENERS.add("shrinke.me");
		URL_SHORTENERS.add("ad.fly");
	}

	public static final Pattern URL_SHORTENER_PATTERN = Pattern.compile("(?:" + URL_SHORTENERS.stream().map(s -> s.replace(".", "\\.")).collect(Collectors.joining("|")) + ")/\\w+", Pattern.MULTILINE);
	public static final Pattern ONLY_SYMBOLS = Pattern.compile("[-0-9()@:%_+.~#?&/=]+");
	public static final Pattern STEAM_PATTERN = Pattern.compile("(st[\\w.]+\\.\\w{2,6}(?:\\.\\w{2,6})?)/_?(?:tr|\\?p|new|app|profile)", Pattern.MULTILINE);
	public static final Pattern NITRO_PATTERN = Pattern.compile("giveawaynitro|nitro-discord\\.\\w+|discord-nitro\\.\\w+|/(?:free|airdrop-)?nitro|turbodlscord|discord(?:nitro)?gift", Pattern.MULTILINE);
	public static final Pattern EXACT_DOMAIN = Pattern.compile("[\\w.-]+");

	public static void fetchDomains(Runnable done) {
		URLRequest.of("https://phish.sinking.yachts/v2/all").addHeader("X-Identity", "gnomebot+" + App.instance.discordHandler.selfId).toJsonArray().subscribeContent(content -> {
			var set = new HashSet<String>();

			for (var e : content) {
				var domain = String.valueOf(e).trim().toLowerCase();

				if (domain.startsWith("www.")) {
					domain = domain.substring(4);
				}

				set.add(domain);
			}

			synchronized (LOCK) {
				lastRemoteUpdate = Instant.now();
				REMOTE_BAD_DOMAINS = set;

				try {
					List<String> list = new ArrayList<>(set);
					list.sort(null);
					list.add(0, lastRemoteUpdate.toString());
					Files.write(AppPaths.BAD_DOMAINS, list);
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				done.run();
			}
		});
	}

	public static void loadDomains() {
		synchronized (LOCK) {
			if (Files.exists(AppPaths.BAD_DOMAIN_OVERRIDES)) {
				try {
					var lines = Files.readAllLines(AppPaths.BAD_DOMAIN_OVERRIDES);
					OVERRIDES.clear();

					for (var s : lines) {
						var split = s.trim().split(": ", 2);

						if (split.length == 2 && !split[0].trim().isEmpty() && !split[1].trim().isEmpty()) {
							var t = Type.MAP.getOrDefault(split[1].trim().toLowerCase(), Type.DEFAULT);

							if (t != Type.DEFAULT) {
								OVERRIDES.put(split[0].trim().toLowerCase(), t);
							}
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else {
				saveDomainsSynced();
			}

			if (Files.exists(AppPaths.BAD_DOMAINS)) {
				try {
					var lines = Files.readAllLines(AppPaths.BAD_DOMAINS);
					lastRemoteUpdate = lines.isEmpty() ? null : Instant.parse(lines.get(0));

					if (lastRemoteUpdate != null) {
						lines.remove(0);
					}

					REMOTE_BAD_DOMAINS = new HashSet<>(lines);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} else {
				fetchDomains(() -> {
				});
			}
		}
	}

	private static void saveDomainsSynced() {
		try {
			Files.write(AppPaths.BAD_DOMAIN_OVERRIDES, OVERRIDES.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> e.getKey() + ": " + e.getValue()).toList());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void setOverride(GuildCollections gc, String user, String domain, Type override) {
		synchronized (LOCK) {
			Log.important("Domain " + domain + " set to " + override + " by " + user + " in " + gc);
			domain = domain.trim().toLowerCase();

			if (domain.startsWith("www.")) {
				domain = domain.substring(4);
			}

			if (override == null || override == Type.DEFAULT) {
				OVERRIDES.remove(domain);
			} else {
				OVERRIDES.put(domain, override);
			}

			saveDomainsSynced();
		}
	}

	public static Type checkScamDomain(String domain) {
		if (domain.startsWith("www.")) {
			domain = domain.substring(4);
		}

		synchronized (LOCK) {
			var o = OVERRIDES.getOrDefault(domain, Type.DEFAULT);

			if (o != Type.DEFAULT) {
				return o;
			} else if (REMOTE_BAD_DOMAINS.contains(domain)) {
				return Type.BLOCK;
			} else if (ONLY_SYMBOLS.matcher(domain).matches()) {
				return Type.ALLOW;
			}

			return Type.DEFAULT;
		}
	}

	@Nullable
	public static Scam checkScam(String content) {
		// Discord now has pretty much eliminated need for manual checks
		if (true) {
			return null;
		}

		var urlMatcher = MessageHandler.URL_PATTERN.matcher(content);

		while (urlMatcher.find()) {
			var url = urlMatcher.group();
			var domain = urlMatcher.group(1);

			if (domain.startsWith("www.")) {
				domain = domain.substring(4);
			}

			if (domain.isEmpty()) {
				continue;
			}

			var override = checkScamDomain(domain);

			if (override == Type.BLOCK) {
				return new Scam(url, domain, "misc");
			} else if (override == Type.ALLOW) {
				continue;
			}

			var nitroScamMatcher = ScamHandler.NITRO_PATTERN.matcher(url);

			if (nitroScamMatcher.find()) {
				return new Scam(nitroScamMatcher.group(), domain, "nitro");
			}

			var steamScamMatcher = ScamHandler.STEAM_PATTERN.matcher(url);

			if (steamScamMatcher.find()) {
				return new Scam(steamScamMatcher.group(), domain, "steam");
			}
		}

		return null;
	}
}
