package dev.gnomebot.app.server.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import dev.gnomebot.app.App;
import dev.gnomebot.app.data.DiscordMessageCount;
import dev.gnomebot.app.discord.command.LeaderboardCommand;
import dev.gnomebot.app.server.HTTPResponseCode;
import dev.gnomebot.app.server.ServerRequest;
import dev.gnomebot.app.server.json.JsonResponse;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
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
		long time = Integer.parseInt(request.variable("days")) * MS_IN_DAY;

		if (time < 0L) {
			throw HTTPResponseCode.BAD_REQUEST.error("Invalid timespan!");
		}

		int limit = request.query("limit").asInt();
		long channel = request.query("channel").asLong();
		Snowflake role = Snowflake.of(request.query("role").asLong());

		Map<Snowflake, Member> memberMap = request.gc.getGuild().getMembers().filter(member -> !member.isBot()).toStream().collect(Collectors.toMap(User::getId, member -> member));
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

		for (Document document : request.gc.messageXp.aggregate(agg)) {
			LeaderboardEntry entry = new LeaderboardEntry();
			entry.userId = document.getLong("_id");
			entry.xp = ((Number) document.get("xp")).longValue();
			leaderboardEntries.add(entry);
		}

		leaderboardEntries.sort(null);

		return JsonResponse.array(array -> {
			for (LeaderboardEntry entry : leaderboardEntries) {
				try {
					Member member = memberMap.get(Snowflake.of(entry.userId));

					if (member == null || role.asLong() != 0L && !member.getRoleIds().contains(role)) {
						continue;
					}

					JsonObject o = new JsonObject();
					o.addProperty("id", member.getId().asString());
					o.addProperty("name", member.getDisplayName());
					o.addProperty("xp", entry.xp);
					o.addProperty("rank", array.size() + 1);
					int col = member.getColor().block().getRGB() & 0xFFFFFF;
					o.addProperty("color", String.format("#%06X", col == 0 ? 0xFFFFFF : col));
					array.add(o);
				} catch (Exception ex) {
				}

				if (limit > 0 && array.size() >= limit) {
					break;
				}
			}
		});
	}

	public static Response leaderboardImage(ServerRequest request) throws Exception {
		long time = Integer.parseInt(request.variable("days")) * MS_IN_DAY;

		if (time < 0L) {
			throw HTTPResponseCode.BAD_REQUEST.error("Invalid timespan!");
		}

		int limit = request.query("limit").asInt(20);
		long channel = request.query("channel").asLong();
		Snowflake role = Snowflake.of(request.query("role").asLong());

		Map<Snowflake, Member> memberMap = request.gc.getGuild().getMembers().filter(member -> !member.isBot()).toStream().collect(Collectors.toMap(User::getId, member -> member));
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

		for (Document document : request.gc.messageXp.aggregate(agg)) {
			LeaderboardEntry entry = new LeaderboardEntry();
			entry.userId = document.getLong("_id");
			entry.xp = ((Number) document.get("xp")).longValue();
			leaderboardEntries.add(entry);
		}

		leaderboardEntries.sort(null);

		List<LeaderboardCommand.LeaderboardCommandEntry> list = new ArrayList<>(limit);

		for (LeaderboardEntry entry : leaderboardEntries) {
			try {
				Member member = memberMap.get(Snowflake.of(entry.userId));

				if (member == null || role.asLong() != 0L && !member.getRoleIds().contains(role)) {
					continue;
				}

				var e = new LeaderboardCommand.LeaderboardCommandEntry();
				e.id = member.getId().asString();
				e.name = member.getDisplayName();
				e.xp = Utils.format(entry.xp);
				e.rank = list.size() + 1;
				int col = member.getColor().block().getRGB() & 0xFFFFFF;
				e.color = col == 0 ? 0xFFFFFF : col;
				list.add(e);
			} catch (Exception ex) {
			}

			if (limit > 0 && list.size() >= limit) {
				break;
			}
		}

		BufferedImage[] avatars = new BufferedImage[list.size()];
		AtomicInteger avatarsRemaining = new AtomicInteger(list.size());

		for (int i = 0; i < list.size(); i++) {
			final int index = i;
			Thread thread = new Thread(() -> {
				try {
					avatars[index] = Utils.internalRequest("api/info/avatar/" + list.get(index).id + "/42").toImage().block();
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
		}

		Font font = new Font(request.gc.font.get(), Font.BOLD, 36);
		FontMetrics metrics = new Canvas().getFontMetrics(font);

		int w = 0;

		for (LeaderboardCommand.LeaderboardCommandEntry entry : list) {
			w = Math.max(w, metrics.stringWidth(entry.name + entry.xp) + 240);
		}

		w = Math.max(w, 50);
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
	}

	private static class MessageInfo {
		public int index;
		public String id;
		public String name;
		public int totalMessages;
		public int[] messages;
	}

	public static Response channels(ServerRequest request) throws Exception {
		boolean fast = request.query("fast").asBoolean(false);

		Map<Snowflake, MessageInfo> channels = new LinkedHashMap<>();

		try {
			request.gc.getChannelList()
					.stream()
					.filter(c -> c.checkPermissions(request.token.userId, Permission.VIEW_CHANNEL))
					.sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()))
					.forEach(c -> {
						MessageInfo mi = new MessageInfo();
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

		final long now = System.currentTimeMillis();
		final long oldestMessage = Date.from(request.gc.guildId.getTimestamp()).getTime();
		int weeks = (int) ((now - oldestMessage) / MS_IN_DAY / 7L);

		if (weeks <= 0) {
			weeks = 1;
		}

		List<Bson> filter = new ArrayList<>();

		for (MessageInfo mi : channels.values()) {
			mi.messages = new int[weeks];
			mi.totalMessages = 0;
			filter.add(Filters.eq("channel", Snowflake.of(mi.id).asLong()));
		}

		for (DiscordMessageCount mc : request.gc.messageCount.query().filter(Filters.or(filter))) {
			int week = (weeks - 1) - (int) ((now - mc.getDate().getTime()) / MS_IN_DAY / 7L);

			if (week >= 0 && week < weeks) {
				MessageInfo mi = channels.get(Snowflake.of(mc.getChannel()));

				if (mi != null) {
					int m = mc.getCount();
					mi.messages[week] += m;
					mi.totalMessages += m;
				}
			}
		}

		List<MessageInfo> channelList = channels.values()
				.stream()
				.filter(messageInfo -> messageInfo.totalMessages > 0)
				.sorted((o1, o2) -> Integer.compare(o2.totalMessages, o1.totalMessages))
				.toList();

		for (int i = 0; i < channelList.size(); i++) {
			channelList.get(i).index = i;
		}

		int w = weeks;

		return JsonResponse.object(json -> {
			json.addProperty("id", request.gc.guildId.asString());
			json.addProperty("name", request.gc.name.get());
			json.addProperty("weeks", w);

			JsonArray infos = new JsonArray();

			for (MessageInfo mi : channelList) {
				JsonObject o = new JsonObject();
				o.addProperty("index", mi.index);
				o.addProperty("id", mi.id);
				o.addProperty("name", mi.name);
				o.addProperty("color", String.format("#%06X", Utils.getChartColor(mi.index)));
				o.addProperty("totalMessages", mi.totalMessages);
				infos.add(o);
			}

			json.add("channels", infos);

			JsonArray messages = new JsonArray();

			for (int i = 0; i < w; i++) {
				if (fast) {
					int[] ai = new int[channelList.size()];

					for (MessageInfo mi : channelList) {
						ai[mi.index] = mi.messages[i];
					}

					JsonArray a = new JsonArray();

					for (int aii : ai) {
						a.add(aii);
					}

					messages.add(a);
				} else {
					JsonObject o = new JsonObject();

					for (MessageInfo mi : channelList) {
						if (mi.messages[i] > 0) {
							o.addProperty(mi.name, mi.messages[i]);
						}
					}

					o.addProperty("_week_", i);
					messages.add(o);
				}
			}

			json.add("messages", messages);
		});
	}

	public static Response members(ServerRequest request) throws Exception {
		boolean fast = request.query("fast").asBoolean(false);

		Map<Snowflake, MessageInfo> members = new LinkedHashMap<>();

		try {
			List<Member> channelList = request.gc.getGuild().getMembers()
					.toStream()
					.sorted((o1, o2) -> o1.getUsername().compareToIgnoreCase(o2.getUsername())).toList();

			for (Member m : channelList) {
				MessageInfo mi = new MessageInfo();
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

		final long now = System.currentTimeMillis();
		final long oldestMessage = Date.from(request.gc.guildId.getTimestamp()).getTime();
		int weeks = (int) ((now - oldestMessage) / MS_IN_DAY / 7L);

		if (weeks <= 0) {
			weeks = 1;
		}

		for (MessageInfo mi : members.values()) {
			mi.messages = new int[weeks];
			mi.totalMessages = 0;
		}

		for (DiscordMessageCount mc : request.gc.messageCount.query()) {
			int week = (weeks - 1) - (int) ((now - mc.getDate().getTime()) / MS_IN_DAY / 7L);

			if (week >= 0 && week < weeks) {
				MessageInfo mi = members.get(Snowflake.of(mc.getUser()));

				if (mi != null) {
					int m = mc.getCount();
					mi.messages[week] += m;
					mi.totalMessages += m;
				}
			}
		}

		List<MessageInfo> memberList = members.values()
				.stream()
				.filter(messageInfo -> messageInfo.totalMessages >= 10)
				.sorted((o1, o2) -> Integer.compare(o2.totalMessages, o1.totalMessages))
				.collect(Collectors.toList());

		for (int i = 0; i < memberList.size(); i++) {
			memberList.get(i).index = i;
		}

		memberList.removeIf(m -> m.index >= 1000);

		int w = weeks;

		return JsonResponse.object(json -> {
			json.addProperty("id", request.gc.guildId.asString());
			json.addProperty("name", request.gc.name.get());
			json.addProperty("weeks", w);

			JsonArray infos = new JsonArray();

			for (MessageInfo mi : memberList) {
				JsonObject o = new JsonObject();
				o.addProperty("index", mi.index);
				o.addProperty("id", mi.id);
				o.addProperty("name", mi.name);
				o.addProperty("color", String.format("#%06X", Utils.getChartColor(mi.index)));
				o.addProperty("totalMessages", mi.totalMessages);
				infos.add(o);
			}

			json.add("members", infos);

			JsonArray messages = new JsonArray();

			for (int i = 0; i < w; i++) {
				if (fast) {
					int[] ai = new int[memberList.size()];

					for (MessageInfo mi : memberList) {
						ai[mi.index] = mi.messages[i];
					}

					JsonArray a = new JsonArray();

					for (int aii : ai) {
						a.add(aii);
					}

					messages.add(a);
				} else {
					JsonObject o = new JsonObject();

					for (MessageInfo mi : memberList) {
						if (mi.messages[i] > 0) {
							o.addProperty(mi.name, mi.messages[i]);
						}
					}

					o.addProperty("_week_", i);
					messages.add(o);
				}
			}

			json.add("messages", messages);
		});
	}
}