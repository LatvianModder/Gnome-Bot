package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.GuildCollections;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.object.entity.Message;

public class MessageInteractionEventWrapper extends ApplicationCommandInteractionEventWrapper<MessageInteractionEvent> {
	public final Message message;

	public MessageInteractionEventWrapper(GuildCollections gc, MessageInteractionEvent e) {
		super(gc, e);
		message = e.getResolvedMessage();
		// event.context.findMessage(event.get("message").asSnowflake()).orElse(null)
	}

	@Override
	public String toString() {
		return event.getCommandName() + " message=" + message.getId().asString();
	}
}
