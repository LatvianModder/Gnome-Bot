package dev.gnomebot.app.data;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.TimeUtils;
import dev.latvian.apps.webutils.ansi.AnsiComponent;
import dev.latvian.apps.webutils.ansi.AnsiJava;
import discord4j.core.object.entity.channel.ThreadChannel;
import discord4j.core.spec.ThreadChannelEditSpec;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.LinkedHashMap;
import java.util.Objects;

public class ScheduledTask extends WrappedDocument<ScheduledTask> {
	public static final String UNMUTE = "unmute";
	public static final String REMIND_ME = "remind_me";
	public static final String CLOSE_THREAD = "close_thread";

	public final String type;
	public long end;
	public final long guildId;
	public final long channelId;
	public final long userId;
	public final String content;

	public ScheduledTask(WrappedCollection<ScheduledTask> c, MapWrapper d) {
		super(c, d);
		this.end = document.getLong("end");
		this.type = document.getString("type");
		this.guildId = document.getLong("guild");
		this.channelId = document.getLong("channel");
		this.userId = document.getLong("user");
		this.content = document.getString("content");
	}

	public ScheduledTask(WrappedCollection<ScheduledTask> c, String type, long end, long guild, long channel, long user, String content) {
		this(c, MapWrapper.wrap(new Document("_id", new ObjectId())
				.append("type", type)
				.append("end", end)
				.append("guild", guild)
				.append("channel", channel)
				.append("user", user)
				.append("content", content)
		));
	}

	public void changeEnd(long end) {
		this.end = end;

		if (end < 0L) {
			delete();
		} else {
			update(Updates.set("end", end));
		}
	}

	public void cancel() {
		changeEnd(-1L);
	}

	public boolean run(long now) throws Exception {
		if (end < 0L) {
			return true;
		}

		if (now >= end) {
			delete();
			var gc = guildId == 0L ? null : collection.db.guild(guildId);

			switch (type) {
				case UNMUTE -> unmuteNow(Objects.requireNonNull(gc), userId, content);
				case REMIND_ME -> App.info("Reminder!");
				case CLOSE_THREAD -> closeThreadNow(Objects.requireNonNull(gc), channelId);
				default -> App.warn("Unknown scheduled task type: " + type);
			}

			return true;
		}

		return false;
	}

	public static void unmuteNow(GuildCollections gc, long userId, String reason) {
		var data = gc.db.app.discordHandler.getUserData(userId);
		App.warn("Unmuting " + gc + "/" + data.id().asString() + "/" + data.username());

		var mutedRole = gc.mutedRole.getRole();

		if (mutedRole != null) {
			mutedRole.remove(userId, "Unmuted");

			gc.adminLogChannelEmbed(data, gc.adminLogChannel, spec -> {
				spec.color(EmbedColor.GREEN);

				if (!reason.isEmpty()) {
					spec.field("Reason", reason);
				}

				spec.author(data.username() + " has been unmuted!", Utils.getAvatarUrl(data));
			});
		}
	}

	public static void closeThreadNow(GuildCollections gc, long channelId) {
		var channel = gc.db.app.discordHandler.client.getChannelById(SnowFlake.convert(channelId)).cast(ThreadChannel.class).block();
		App.info("Closing thread " + channelId + "/" + channel.getName());
		channel.edit(ThreadChannelEditSpec.builder().archived(true).build()).subscribe();
	}

	public AnsiComponent toAnsi() {
		var map = new LinkedHashMap<String, Object>();
		map.put("type", type);
		map.put("expires", TimeUtils.prettyTimeString((end - System.currentTimeMillis()) / 1000L));
		map.put("guild", guildId);
		map.put("channel", channelId);
		map.put("user", userId);
		map.put("content", content);
		return AnsiJava.of(map);
	}

	@Override
	public String toString() {
		return toAnsi().toUnformattedString();
	}
}