package dev.gnomebot.app.cli;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.core.object.entity.Member;

public abstract class CLIEvent {
	public final GuildCollections gc;
	public final Member sender;
	public final CommandReader reader;
	public final MessageBuilder response;

	public CLIEvent(GuildCollections g, Member m, CommandReader r) {
		gc = g;
		sender = m;
		reader = r;
		response = MessageBuilder.create();
	}

	public abstract void respond(String text);
}
