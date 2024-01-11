package dev.gnomebot.app.server.handler;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.command.LeaderboardCommandEntry;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.FormattingUtils;
import dev.latvian.apps.webutils.canvas.ImageCanvas;
import dev.latvian.apps.webutils.data.Color4f;
import dev.latvian.apps.webutils.json.JSONArray;
import dev.latvian.apps.webutils.json.JSONObject;
import dev.latvian.apps.webutils.json.JSONResponse;
import dev.latvian.apps.webutils.net.FileResponse;
import dev.latvian.apps.webutils.net.Response;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import org.bson.conversions.Bson;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ActivityHandlers {
	public static final long MS_IN_DAY = 86400000L;

	public static class LeaderboardEntry implements Comparable<LeaderboardEntry> {
		public long userId;
		public long xp;

		@Override
		public int compareTo(LeaderboardEntry o) {
			if (xp == o.xp) {
				return Long.compare(userId, o.userId);
			}

			return Long.compare(o.xp, xp);
		}
	}

	public static Response leaderboard(ServerRequest request) throws Exception {
		var time = Integer.parseInt(request.variable("days")) * MS_IN_DAY;

		if (time < 0L) {
			throw HTTPResponseCode.BAD_REQUEST.error("Invalid timespan!");
		}

		var limit = request.query("limit").asInt();
		var channel = request.query("channel").asLong();
		var role = Snowflake.of(request.query("role").asLong());

		var memberMap = request.gc.getGuild().getMembers().filter(member -> !member.isBot()).toStream().collect(Collectors.toMap(User::getId, member -> member));
		List<LeaderboardEntry> leaderboardEntries = new ArrayList<>();
		List<Bson> agg = new ArrayList<>();
		List<Bson> filter = new ArrayList<>();

		if (channel != 0L) {
			filter.add(Filters.eq("channel", channel));
		}

		if (time > 0L) {
			filter.add(Filters.gt("date", new Date(System.currentTimeMillis() - time)));
		}

		if (filter.size() == 1) {
			agg.add(Aggregates.match(filter.get(0)));
		} else if (!filter.isEmpty()) {
			agg.add(Aggregates.match(Filters.and(filter)));
		}

		agg.add(Aggregates.group("$user", Accumulators.sum("xp", "$xp")));

		for (var document : request.gc.messageXp.aggregate(agg)) {
			var entry = new LeaderboardEntry();
			entry.userId = document.getLong("_id");
			entry.xp = ((Number) document.get("xp")).longValue();
			leaderboardEntries.add(entry);
		}

		leaderboardEntries.sort(null);

		var array = JSONArray.of();

		for (var entry : leaderboardEntries) {
			try {
				var member = memberMap.get(Snowflake.of(entry.userId));

				if (member == null || role.asLong() != 0L && !member.getRoleIds().contains(role)) {
					continue;
				}

				var o = array.addObject();
				o.put("id", member.getId().asString());
				o.put("name", member.getDisplayName());
				o.put("xp", entry.xp);
				o.put("rank", array.size() + 1);
				var col = member.getColor().block().getRGB() & 0xFFFFFF;
				o.put("color", String.format("#%06X", col == 0 ? 0xFFFFFF : col));
			} catch (Exception ex) {
			}

			if (limit > 0 && array.size() >= limit) {
				break;
			}
		}

		return JSONResponse.of(array);
	}

	public static Response leaderboardImage(ServerRequest request) throws Exception {
		var time = Integer.parseInt(request.variable("days")) * MS_IN_DAY;

		if (time < 0L) {
			throw HTTPResponseCode.BAD_REQUEST.error("Invalid timespan!");
		}

		var limit = request.query("limit").asInt(20);
		var channel = request.query("channel").asLong();
		var role = Snowflake.of(request.query("role").asLong());

		var memberMap = request.gc.getGuild().getMembers().filter(member -> !member.isBot()).toStream().collect(Collectors.toMap(User::getId, member -> member));
		List<LeaderboardEntry> leaderboardEntries = new ArrayList<>();
		List<Bson> agg = new ArrayList<>();
		List<Bson> filter = new ArrayList<>();

		if (channel != 0L) {
			filter.add(Filters.eq("channel", channel));
		}

		if (time > 0L) {
			filter.add(Filters.gt("date", new Date(System.currentTimeMillis() - time)));
		}

		if (filter.size() == 1) {
			agg.add(Aggregates.match(filter.get(0)));
		} else if (!filter.isEmpty()) {
			agg.add(Aggregates.match(Filters.and(filter)));
		}

		agg.add(Aggregates.group("$user", Accumulators.sum("xp", "$xp")));

		for (var document : request.gc.messageXp.aggregate(agg)) {
			var entry = new LeaderboardEntry();
			entry.userId = document.getLong("_id");
			entry.xp = ((Number) document.get("xp")).longValue();
			leaderboardEntries.add(entry);
		}

		leaderboardEntries.sort(null);

		List<LeaderboardCommandEntry> list = new ArrayList<>(limit);

		for (var entry : leaderboardEntries) {
			try {
				var member = memberMap.get(Snowflake.of(entry.userId));

				if (member == null || role.asLong() != 0L && !member.getRoleIds().contains(role)) {
					continue;
				}

				var e = new LeaderboardCommandEntry();
				e.id = member.getId();
				e.name = member.getDisplayName();
				e.xp = FormattingUtils.format(entry.xp);
				e.rank = list.size() + 1;
				var col = member.getColor().block().getRGB() & 0xFFFFFF;
				e.color = col == 0 ? 0xFFFFFF : col;
				list.add(e);
			} catch (Exception ex) {
			}

			if (limit > 0 && list.size() >= limit) {
				break;
			}
		}

		if (list.isEmpty()) {
			throw HTTPResponseCode.BAD_REQUEST.error("Leaderboard is completely empty!");
		}

		var avatars = new BufferedImage[list.size()];
		var avatarsRemaining = new AtomicInteger(list.size());

		for (var i = 0; i < list.size(); i++) {
			final var index = i;
			var thread = new Thread(() -> {
				try {
					avatars[index] = Utils.getAvatar(list.get(index).id, 42);
				} catch (Exception ex) {
					avatars[index] = new BufferedImage(42, 42, BufferedImage.TYPE_INT_RGB);
					App.error(ex.toString());
				}

				avatarsRemaining.decrementAndGet();
			});

			thread.setDaemon(true);
			thread.start();
		}

		while (avatarsRemaining.get() > 0) {
			Thread.sleep(10L);
		}

		var canvas = new ImageCanvas();
		canvas.setFont(request.gc.font.create(36));

		var w = 0;

		for (var entry : list) {
			w = Math.max(w, canvas.metrics.stringWidth(entry.name + entry.xp) + 240);
		}

		w = Math.max(w, 50);

		/*
		int h = Math.max(list.size() * 45, 45);

		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setBackground(new Color(0x36393F));
		g.clearRect(0, 0, w, h);
		g.setFont(font);

		String indexFormat = "#%0" + String.valueOf(list.size()).length() + "d";

		for (int i = 0; i < list.size(); i++) {
			LeaderboardCommand.LeaderboardCommandEntry entry = list.get(i);
			g.setColor(Color.GRAY);
			g.drawString(String.format(indexFormat, entry.rank), 6, 36 + i * 45);
			g.setColor(new Color(entry.color));
			g.drawString(entry.name, 151, 36 + i * 45);
			g.setColor(Color.WHITE);
			g.drawString(entry.xp, w - 6 - metrics.stringWidth(entry.xp), 36 + i * 45);
			g.drawImage(avatars[i], 100, 3 + i * 45, 42, 42, null);
		}

		g.dispose();
		return FileResponse.image(image);
		 */

		var indexFormat = "#%0" + String.valueOf(list.size()).length() + "d";

		for (var i = 0; i < list.size(); i++) {
			var entry = list.get(i);
			canvas.addString(6, 36 + i * 45, String.format(indexFormat, entry.rank), Color.GRAY);
			canvas.addString(151, 36 + i * 45, entry.name, new Color(entry.color));
			canvas.addString(w - 6 - canvas.metrics.stringWidth(entry.xp), 36 + i * 45, entry.xp, Color.WHITE);
			canvas.addImage(100, 3 + i * 45, 42, 42, avatars[i]);
		}

		return FileResponse.png(canvas.createImage());
	}

	public static Response rank(ServerRequest request) throws Exception {
		var time = Integer.parseInt(request.variable("days")) * MS_IN_DAY;

		if (time < 0L) {
			throw HTTPResponseCode.BAD_REQUEST.error("Invalid timespan!");
		}

		var member = Utils.snowflake(request.variable("member")).asLong();
		var channel = request.query("channel").asLong();

		var json = JSONObject.of();

		json.put("id", Snowflake.asString(member));
		json.put("name", "Unknown");
		json.put("xp", 0);
		json.put("rank", 0);
		// int col = member.getColor().block().getRGB() & 0xFFFFFF;
		// json.addProperty("color", String.format("#%06X", col == 0 ? 0xFFFFFF : col));
		json.put("color", String.format("#%06X", 0xFFFFFF));

		var fields = json.addArray("fields");

			/*
			try {
				JsonArray leaderboardJson = Utils.internalRequest("api/guild/activity/leaderboard/" + event.context.gc.guildId.asString() + "/" + days).timeout(5000).toJson().block().getAsJsonArray();
				String id = m.getId().asString();

				for (JsonElement e : leaderboardJson) {
					JsonObject o = e.getAsJsonObject();

					if (o.get("id").getAsString().equals(id)) {
						// event.response().createFollowupMessage("**Rank:**  #0   |   **XP:**  0").subscribe();

						EmbedBuilder embed = EmbedBuilder.create();
						embed.author(m.getDisplayName(), m.getAvatarUrl());

						if (o.get("rank").getAsInt() == 69) {
							embed.inlineField("Rank", "#69, nice");
						} else {
							embed.inlineField("Rank", "#" + o.get("rank").getAsInt());
						}

						embed.inlineField("XP", Utils.format(o.get("xp").getAsLong()));

						if (event.context.gc.isMM() && event.context.gc.regularMessages.get() > 0 && !event.context.gc.regularRole.is(m)) {
							long totalMessages = event.context.gc.members.findFirst(m).getTotalMessages();

							if (totalMessages < event.context.gc.regularMessages.get()) {
								if (totalMessages < MessageHandler.MM_MEMBER) {
									embed.inlineField("Member Rank", ((long) (totalMessages * 10000D / (double) MessageHandler.MM_MEMBER) / 100D) + "%");
								} else {
									embed.inlineField("Regular Rank", ((long) (totalMessages * 10000D / (double) event.context.gc.regularMessages.get()) / 100D) + "%");
								}
							}
						}

						event.respond(embed);
						return;
					}
				}

				event.respond(EmbedBuilder.create()
						.author(m.getDisplayName(), m.getAvatarUrl())
						.inlineField("Rank", "Unranked")
						.inlineField("XP", "0")
				);
			} catch (Exception ex) {
				ex.printStackTrace();
				event.respond("Failed to connect to API!");
			}
			 */

		return JSONResponse.of(json);
	}

	private static class MessageInfo {
		public int index;
		public String id;
		public String name;
		public int totalMessages;
		public int[] messages;
	}

	public static Response channels(ServerRequest request) throws Exception {
		var fast = request.query("fast").asBoolean(false);

		Map<Snowflake, MessageInfo> channels = new LinkedHashMap<>();

		try {
			request.gc.getChannelList()
					.stream()
					.filter(c -> c.canViewChannel(request.token.userId))
					.sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
					.forEach(c -> {
						var mi = new MessageInfo();
						mi.id = c.id.asString();
						mi.name = c.getName();
						channels.put(c.id, mi);
					});
		} catch (Exception ex) {
			throw HTTPResponseCode.NOT_FOUND.error("Channel not found!");
		}

		if (channels.isEmpty()) {
			throw HTTPResponseCode.NOT_FOUND.error("Channels not found!");
		}

		final var now = System.currentTimeMillis();
		final var oldestMessage = Date.from(request.gc.guildId.getTimestamp()).getTime();
		var weeks = (int) ((now - oldestMessage) / MS_IN_DAY / 7L);

		if (weeks <= 0) {
			weeks = 1;
		}

		List<Bson> filter = new ArrayList<>();

		for (var mi : channels.values()) {
			mi.messages = new int[weeks];
			mi.totalMessages = 0;
			filter.add(Filters.eq("channel", Utils.snowflake(mi.id).asLong()));
		}

		for (var mc : request.gc.messageCount.query().filter(Filters.or(filter))) {
			var week = (weeks - 1) - (int) ((now - mc.getDate().getTime()) / MS_IN_DAY / 7L);

			if (week >= 0 && week < weeks) {
				var mi = channels.get(Snowflake.of(mc.getChannel()));

				if (mi != null) {
					var m = mc.getCount();
					mi.messages[week] += m;
					mi.totalMessages += m;
				}
			}
		}

		var channelList = channels.values()
				.stream()
				.filter(messageInfo -> messageInfo.totalMessages > 0)
				.sorted((o1, o2) -> Integer.compare(o2.totalMessages, o1.totalMessages))
				.toList();

		for (var i = 0; i < channelList.size(); i++) {
			channelList.get(i).index = i;
		}

		var w = weeks;

		var json = JSONObject.of();
		json.put("id", request.gc.guildId.asString());
		json.put("name", request.gc.name);
		json.put("weeks", w);

		var infos = json.addArray("channels");

		for (var mi : channelList) {
			var o = infos.addObject();
			o.put("index", mi.index);
			o.put("id", mi.id);
			o.put("name", mi.name);
			o.put("color", String.format("#%06X", Color4f.getChartColor(mi.index).rgb()));
			o.put("totalMessages", mi.totalMessages);
		}

		var messages = json.addArray("messages");

		for (var i = 0; i < w; i++) {
			if (fast) {
				var ai = new int[channelList.size()];

				for (var mi : channelList) {
					ai[mi.index] = mi.messages[i];
				}

				var a = messages.addArray();

				for (var aii : ai) {
					a.add(aii);
				}
			} else {
				var o = messages.addObject();

				for (var mi : channelList) {
					if (mi.messages[i] > 0) {
						o.put(mi.name, mi.messages[i]);
					}
				}

				o.put("_week_", i);
			}
		}

		return JSONResponse.of(json);
	}

	public static Response members(ServerRequest request) throws Exception {
		var fast = request.query("fast").asBoolean(false);

		Map<Snowflake, MessageInfo> members = new LinkedHashMap<>();

		try {
			var channelList = request.gc.getGuild().getMembers()
					.toStream()
					.sorted((o1, o2) -> o1.getUsername().compareToIgnoreCase(o2.getUsername())).toList();

			for (var m : channelList) {
				var mi = new MessageInfo();
				mi.id = m.getId().asString();
				mi.name = m.getDisplayName();
				members.put(m.getId(), mi);
			}
		} catch (Exception ex) {
			throw HTTPResponseCode.NOT_FOUND.error("Member not found!");
		}

		if (members.isEmpty()) {
			throw HTTPResponseCode.NOT_FOUND.error("Members not found!");
		}

		final var now = System.currentTimeMillis();
		final var oldestMessage = Date.from(request.gc.guildId.getTimestamp()).getTime();
		var weeks = (int) ((now - oldestMessage) / MS_IN_DAY / 7L);

		if (weeks <= 0) {
			weeks = 1;
		}

		for (var mi : members.values()) {
			mi.messages = new int[weeks];
			mi.totalMessages = 0;
		}

		for (var mc : request.gc.messageCount.query()) {
			var week = (weeks - 1) - (int) ((now - mc.getDate().getTime()) / MS_IN_DAY / 7L);

			if (week >= 0 && week < weeks) {
				var mi = members.get(Snowflake.of(mc.getUser()));

				if (mi != null) {
					var m = mc.getCount();
					mi.messages[week] += m;
					mi.totalMessages += m;
				}
			}
		}

		var memberList = members.values()
				.stream()
				.filter(messageInfo -> messageInfo.totalMessages >= 10)
				.sorted((o1, o2) -> Integer.compare(o2.totalMessages, o1.totalMessages))
				.collect(Collectors.toList());

		for (var i = 0; i < memberList.size(); i++) {
			memberList.get(i).index = i;
		}

		memberList.removeIf(m -> m.index >= 1000);

		var w = weeks;

		var json = JSONObject.of();
		json.put("id", request.gc.guildId.asString());
		json.put("name", request.gc.name);
		json.put("weeks", w);

		var infos = json.addArray("members");

		for (var mi : memberList) {
			var o = infos.addObject();
			o.put("index", mi.index);
			o.put("id", mi.id);
			o.put("name", mi.name);
			o.put("color", String.format("#%06X", Color4f.getChartColor(mi.index).rgb()));
			o.put("totalMessages", mi.totalMessages);
		}

		var messages = json.addArray("messages");

		for (var i = 0; i < w; i++) {
			if (fast) {
				var ai = new int[memberList.size()];

				for (var mi : memberList) {
					ai[mi.index] = mi.messages[i];
				}

				var a = messages.addArray();
				for (var aii : ai) {
					a.add(aii);
				}
			} else {
				var o = messages.addObject();

				for (var mi : memberList) {
					if (mi.messages[i] > 0) {
						o.put(mi.name, mi.messages[i]);
					}
				}

				o.put("_week_", i);
			}
		}

		return JSONResponse.of(json);
	}

	public static Response userMentionLeaderboardImage(ServerRequest request) throws Exception {
		return mentionLeaderboardImage(request, true);
	}

	public static Response roleMentionLeaderboardImage(ServerRequest request) throws Exception {
		return mentionLeaderboardImage(request, false);
	}

	private static Response mentionLeaderboardImage(ServerRequest request, boolean isUser) throws Exception {
		var time = Integer.parseInt(request.variable("days")) * MS_IN_DAY;

		if (time < 0L) {
			throw HTTPResponseCode.BAD_REQUEST.error("Invalid timespan!");
		}

		var mentionId = Long.parseUnsignedLong(request.variable("mention"));

		var limit = request.query("limit").asInt(20);
		var channel = request.query("channel").asLong();

		var memberMap = request.gc.getGuild().getMembers().filter(member -> !member.isBot()).toStream().collect(Collectors.toMap(User::getId, member -> member));
		List<LeaderboardEntry> leaderboardEntries = new ArrayList<>();
		List<Bson> agg = new ArrayList<>();
		List<Bson> filter = new ArrayList<>();

		if (channel != 0L) {
			filter.add(Filters.eq("channel", channel));
		}

		if (time > 0L) {
			filter.add(Filters.gt("date", new Date(System.currentTimeMillis() - time)));
		}


		if (filter.size() == 1) {
			agg.add(Aggregates.match(filter.get(0)));
		} else if (!filter.isEmpty()) {
			agg.add(Aggregates.match(Filters.and(filter)));
		}

		agg.add(Aggregates.group("$user", Accumulators.sum("xp", "$xp")));

		for (var document : request.gc.messageXp.aggregate(agg)) {
			var entry = new LeaderboardEntry();
			entry.userId = document.getLong("_id");
			entry.xp = ((Number) document.get("xp")).longValue();
			leaderboardEntries.add(entry);
		}

		leaderboardEntries.sort(null);

		var list = new ArrayList<LeaderboardCommandEntry>(limit);

		for (var entry : leaderboardEntries) {
			try {
				var member = memberMap.get(Snowflake.of(entry.userId));

				if (member == null) {
					continue;
				}

				var e = new LeaderboardCommandEntry();
				e.id = member.getId();
				e.name = member.getDisplayName();
				e.xp = FormattingUtils.format(entry.xp);
				e.rank = list.size() + 1;
				var col = member.getColor().block().getRGB() & 0xFFFFFF;
				e.color = col == 0 ? 0xFFFFFF : col;
				list.add(e);
			} catch (Exception ex) {
			}

			if (limit > 0 && list.size() >= limit) {
				break;
			}
		}

		if (list.isEmpty()) {
			throw HTTPResponseCode.BAD_REQUEST.error("Leaderboard is completely empty!");
		}

		var avatars = new BufferedImage[list.size()];
		var avatarsRemaining = new AtomicInteger(list.size());

		for (var i = 0; i < list.size(); i++) {
			final var index = i;
			var thread = new Thread(() -> {
				try {
					avatars[index] = Utils.getAvatar(list.get(index).id, 42);
				} catch (Exception ex) {
					avatars[index] = new BufferedImage(42, 42, BufferedImage.TYPE_INT_RGB);
					App.error(ex.toString());
				}

				avatarsRemaining.decrementAndGet();
			});

			thread.setDaemon(true);
			thread.start();
		}

		while (avatarsRemaining.get() > 0) {
			Thread.sleep(10L);
		}

		var canvas = new ImageCanvas();
		canvas.setFont(request.gc.font.create(36));

		var w = 0;

		for (var entry : list) {
			w = Math.max(w, canvas.metrics.stringWidth(entry.name + entry.xp) + 240);
		}

		w = Math.max(w, 50);

		/*
		int h = Math.max(list.size() * 45, 45);

		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setBackground(new Color(0x36393F));
		g.clearRect(0, 0, w, h);
		g.setFont(font);

		String indexFormat = "#%0" + String.valueOf(list.size()).length() + "d";

		for (int i = 0; i < list.size(); i++) {
			LeaderboardCommand.LeaderboardCommandEntry entry = list.get(i);
			g.setColor(Color.GRAY);
			g.drawString(String.format(indexFormat, entry.rank), 6, 36 + i * 45);
			g.setColor(new Color(entry.color));
			g.drawString(entry.name, 151, 36 + i * 45);
			g.setColor(Color.WHITE);
			g.drawString(entry.xp, w - 6 - metrics.stringWidth(entry.xp), 36 + i * 45);
			g.drawImage(avatars[i], 100, 3 + i * 45, 42, 42, null);
		}

		g.dispose();
		return FileResponse.image(image);
		 */

		var indexFormat = "#%0" + String.valueOf(list.size()).length() + "d";

		for (var i = 0; i < list.size(); i++) {
			var entry = list.get(i);
			canvas.addString(6, 36 + i * 45, String.format(indexFormat, entry.rank), Color.GRAY);
			canvas.addString(151, 36 + i * 45, entry.name, new Color(entry.color));
			canvas.addString(w - 6 - canvas.metrics.stringWidth(entry.xp), 36 + i * 45, entry.xp, Color.WHITE);
			canvas.addImage(100, 3 + i * 45, 42, 42, avatars[i]);
		}

		return FileResponse.png(canvas.createImage());
	}
}