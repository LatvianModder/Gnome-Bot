package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.discordjson.json.MessageData;

import java.util.Optional;

public class ComponentEventWrapper extends DeferrableInteractionEventWrapper<ComponentInteractionEvent> {
	public final String[] path;

	public ComponentEventWrapper(GuildCollections gc, ComponentInteractionEvent event, String id) {
		super(gc, event);

		path = id.split("/");

		for (var i = 0; i < path.length; i++) {
			path[i] = decode(path[i]);
		}
	}

	@Override
	public String toString() {
		return String.join("/", path);
	}

	@Override
	public void acknowledge() {
		if (!acknowledged) {
			acknowledged = true;
			event.deferEdit().subscribe();
		}
	}

	@Override
	public void acknowledgeEphemeral() {
		if (!acknowledged) {
			acknowledged = true;
			event.deferEdit().withEphemeral(true).subscribe();
		}
	}

	@Override
	public Optional<MessageData> respond(MessageBuilder messageBuilder) {
		if (edit) {
			event.edit(messageBuilder.toInteractionApplicationCommandCallbackSpec()).block();
		} else {
			event.reply(messageBuilder.toInteractionApplicationCommandCallbackSpec()).block();
		}

		acknowledged = true;
		return Optional.empty();
	}

	@Override
	public boolean requiresTextResponse() {
		return false;
	}

	@Override
	public void delete() {
		event.deleteReply().subscribe();
	}
}