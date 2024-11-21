package dev.gnomebot.app.cli;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.core.object.entity.Member;

public abstract class CLIEvent {
	public final App app;
	public final GuildCollections gc;
	public final Member sender;
	public final CommandReader reader;
	public MessageBuilder response;

	public CLIEvent(App app, GuildCollections g, Member m, CommandReader r) {
		this.app = app;
		this.gc = g;
		this.sender = m;
		this.reader = r;
		this.response = MessageBuilder.create();
	}

	public abstract void respond(String text);
}
