package dev.gnomebot.app.script;

import discord4j.core.object.entity.channel.ThreadChannel;

public class WrappedThread extends WrappedChannel {
	public WrappedThread(WrappedGuild g, ThreadChannel c) {
		super(new WrappedId(c.getId()), g);
		name = c.getName();
		topic = "";
		nsfw = Boolean.FALSE;
	}

	@Override
	public String toString() {
		return "#" + getName();
	}

	public String getMention() {
		return "<#" + id.asString() + ">";
	}
}
