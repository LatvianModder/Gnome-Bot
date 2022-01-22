package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.legacycommand.CommandContext;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.rest.interaction.InteractionResponse;

import java.time.Instant;

public abstract class InteractionEventWrapper<T extends InteractionCreateEvent> {
	public static String encode(String string) {
		return string.replace("/", "&sol;");
	}

	public static String decode(String string) {
		return string.replace("&sol;", "/");
	}

	public final T event;
	public final CommandContext context;

	public InteractionEventWrapper(GuildCollections gc, T e) {
		event = e;
		context = createContext();
		context.handler = gc.db.app.discordHandler;
		context.gc = gc;
		context.channelInfo = gc.getOrMakeChannelInfo(e.getInteraction().getChannelId());
		context.message = event.getInteraction().getMessage().orElse(null);
		context.sender = event.getInteraction().getMember().get();
		context.interaction = this;
	}

	protected CommandContext createContext() {
		return new CommandContext();
	}

	public Instant getTimestamp() {
		return event.getInteraction().getId().getTimestamp();
	}

	public InteractionResponse getResponse() {
		if (event instanceof DeferrableInteractionEvent e) {
			return e.getInteractionResponse();
		}

		throw new IllegalStateException("Response not supported in this type of interaction!");
	}

	public boolean isUserInteraction() {
		return event instanceof UserInteractionEvent;
	}

	public boolean isMessageInteraction() {
		return event instanceof MessageInteractionEvent;
	}
}