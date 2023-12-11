package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;

import java.util.Date;

public class DiscordMessageXP extends WrappedDocument<DiscordMessageXP> {
	public DiscordMessageXP(WrappedCollection<DiscordMessageXP> c, MapWrapper d) {
		super(c, d);
	}

	public long getChannel() {
		return document.getLong("channel");
	}

	public long getUser() {
		return document.getLong("user");
	}

	public int getXP() {
		return document.getInt("xp");
	}

	@Override
	public Date getDate() {
		return document.getDate("date");
	}
}