package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;

import java.util.Date;

/**
 * @author LatvianModder
 */
public class DiscordMessageCount extends WrappedDocument<DiscordMessageCount> {
	public DiscordMessageCount(WrappedCollection<DiscordMessageCount> c, MapWrapper d) {
		super(c, d);
	}

	public long getChannel() {
		return document.getLong("channel");
	}

	public long getUser() {
		return document.getLong("user");
	}

	public int getCount() {
		return document.getInt("count");
	}

	@Override
	public Date getDate() {
		return document.getDate("date");
	}
}