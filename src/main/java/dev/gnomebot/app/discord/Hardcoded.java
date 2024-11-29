package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.ansi.log.Log;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Hardcoded {
	public static final Pattern RULE_REGEX = Pattern.compile("^\\?\\?\\s*rule\\s*(\\d+)$", Pattern.CASE_INSENSITIVE);
	public static final Pattern GREET_REGEX = Pattern.compile("^(hello|hi|hey|greetings|good(?: morning| night| evening| day|bye))$", Pattern.CASE_INSENSITIVE);
	public static Map<String, String> ruleCache = null;

	public static Map<String, String> getRuleCache(GuildCollections gc) {
		if (ruleCache == null) {
			var map = new HashMap<String, String>();

			try {
				var pattern = Pattern.compile("^\\d+\\.\\s+(.+)$");
				var content = gc.getChannelInfo(220243899191394304L).getMessage(1183803614834204764L).getContent();

				for (var line : content.split("\n")) {
					line = line.trim();
					var m = pattern.matcher(line);
					Log.debug("Line " + line);

					if (m.find()) {
						var r = m.group(1).trim();

						if (!r.isEmpty()) {
							map.put(String.valueOf(map.size() + 1), r);
							Log.debug("Rule " + (map.size() + 1) + r);
						}
					}
				}
			} catch (Exception ignore) {
			}

			Log.debug("Loaded MM rules: " + map);
			ruleCache = map;
		}

		return ruleCache;
	}

	public static boolean message(GuildCollections gc, Message message, ChannelInfo channel, Member member, String content) {
		if (gc.isMM()) {
			var m = RULE_REGEX.matcher(content);

			if (m.find()) {
				var key = m.group(1);
				var rule = getRuleCache(gc).getOrDefault(key, "");

				if (rule.isEmpty()) {
					channel.createMessage("That rule doesn't exist...").subscribe();
				} else {
					channel.createMessage("**[Rule #" + key + "](<https://discord.com/channels/166630061217153024/220243899191394304/1183803614834204764>)**\n" + rule).subscribe();
				}

				return true;
			}

			if (GREET_REGEX.matcher(content).find()) {
				message.addReaction(ReactionEmoji.unicode("👋")).subscribe();
				return false;
			}
		}

		return false;
	}

	public static boolean afterMessage(GuildCollections gc, Message message, Member author, long totalMessages, long totalXp, String content) {
		if (gc.isMM() && totalMessages > 300 && !author.getRoleIds().contains(SnowFlake.convert(748075791790637078L))) {
			author.addRole(SnowFlake.convert(748075791790637078L), "Reached Member").subscribe();
		}

		return false;
	}

	public static void messageEdited(GuildCollections gc, MessageUpdateEvent event) {
		if (event.getMessageId().asLong() == 1183803614834204764L) {
			ruleCache = null;
		}
	}
}
