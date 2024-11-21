package dev.gnomebot.app.data;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.util.MapWrapper;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;

public class DiscordPoll extends WrappedDocument<DiscordPoll> {
	public DiscordPoll(WrappedCollection<DiscordPoll> c, MapWrapper d) {
		super(c, d);
	}

	public long getAuthor() {
		return document.getLong("author");
	}

	@Override
	public Date getDate() {
		return document.getDate("timestamp");
	}

	public int getNumber() {
		return document.getInt("number");
	}

	public String getContent() {
		return document.getString("content");
	}

	public List<String> getOptions() {
		return document.getList("options");
	}

	public MapWrapper getVotes() {
		return document.getMap("votes");
	}

	public int getVote(String key) {
		return getVotes().getInt(key, -1);
	}

	public boolean setVote(String key, int vote) {
		var v = getVote(key);

		if (vote != v) {
			if (vote == -1) {
				getVotes().map.remove(key);
				update(Updates.unset("votes." + key));
			} else {
				getVotes().map.put(key, vote);
				update("votes." + key, vote);
			}

			return true;
		}

		return false;
	}

	public EmbedCreateSpec edit(GuildCollections gc, @Nullable EmbedCreateFields.Footer footer) {
		var builder = EmbedCreateSpec.builder();

		var options = getOptions();
		var v = new int[options.size()];

		for (var o : getVotes().map.values()) {
			v[((Number) o).intValue()]++;
		}

		builder.title("Poll #" + getNumber());
		builder.url(gc.db.app.url("poll/" + gc.guildId + "/" + getNumber()));
		builder.description(getContent());

		var sum = 0;

		for (var i : v) {
			sum += i;
		}

		sum = Math.max(1, sum);

		for (var i = 0; i < v.length; i++) {
			builder.addField(Emojis.NUMBER_STRINGS[i] + " " + options.get(i), "**" + v[i] + "** [" + (v[i] * 100 / sum) + "%]", true);
		}

		builder.color(EmbedColor.GRAY);
		builder.footer(footer);
		builder.timestamp(getDate().toInstant());

		return builder.build();
	}
}