package dev.gnomebot.app.data.channel;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.json.JSONObject;
import discord4j.core.object.entity.channel.CategorizableChannel;
import discord4j.core.object.entity.channel.ForumChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CachedChannels implements Iterable<TopLevelChannelInfo> {
	public final GuildCollections gc;
	public final List<TopLevelChannelInfo> list;
	private final Map<Long, TopLevelChannelInfo> map;
	public final Map<String, TopLevelChannelInfo> uniqueNameMap;

	public CachedChannels(GuildCollections gc) {
		this.gc = gc;
		this.list = new ArrayList<>();
		this.map = new LinkedHashMap<>();
		this.uniqueNameMap = new LinkedHashMap<>();

		for (var ch : gc.getGuild().getChannels()
				.ofType(CategorizableChannel.class)
				.sort(Comparator.comparing(CategorizableChannel::getRawPosition).thenComparing(CategorizableChannel::getId))
				.toIterable()
		) {
			var id = ch.getId().asLong();
			var name = ch.getName();
			var settings = gc.db.channelSettings(ch.getId().asLong());

			switch (ch) {
				case ForumChannel ignored -> list.add(new ForumChannelInfo(gc, id, "#" + name, settings));
				case VoiceChannel ignored -> list.add(new TopLevelChannelInfo(gc, id, name, settings));
				case MessageChannel ignored -> list.add(new TopLevelChannelInfo(gc, id, "#" + name, settings));
				default -> Log.warn("Unknown channel type: " + gc + "/" + id + "/" + name + ": " + ch.getClass());
			}
		}

		for (var c : list) {
			map.put(c.id, c);

			var name = c.getName().toLowerCase().replace(' ', '-');
			var key = name;

			for (var i = 2; uniqueNameMap.containsKey(key); i++) {
				key = name + '-' + i;
			}

			uniqueNameMap.put(key, c);
		}

		Log.debug("Cached channels for " + gc + " updated");
	}

	public String name(long id) {
		var ri = map.get(id);
		return ri == null ? SnowFlake.str(id) : ri.getName();
	}

	public String displayName(long id) {
		return "#" + name(id);
	}

	@Nullable
	public TopLevelChannelInfo get(long id) {
		return map.get(id);
	}

	@Override
	@NotNull
	public Iterator<TopLevelChannelInfo> iterator() {
		return list.iterator();
	}

	public JSONObject json(long channel) {
		var json = JSONObject.of();
		json.put("id", SnowFlake.str(channel));
		json.put("name", displayName(channel));
		return json;
	}

	public ChannelInfo getChannelOrThread(long id) {
		var ci = get(id);

		if (ci == null) {
			var data = gc.getClient().getRestClient().getChannelService().getChannel(id).block();
			var parentId = data == null ? null : data.parentId().toOptional().orElse(Optional.empty()).orElse(null);
			var parent = parentId == null ? null : get(parentId.asLong());

			if (parent != null) {
				return parent.thread(id, data.name().toOptional().orElse("unknown-thread"));
			}

			throw new GnomeException("Channel not found");
		}

		return ci;
	}
}
