package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.component.LayoutComponent;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.interaction.InteractionResponse;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public abstract class DeferrableInteractionEventWrapper<T extends DeferrableInteractionEvent> extends InteractionEventWrapper<T> {
	protected boolean acknowledged;

	public DeferrableInteractionEventWrapper(GuildCollections gc, T e) {
		super(gc, e);
	}

	@Override
	public String toString() {
		return event.getClass().getName();
	}

	@Override
	public InteractionResponse getResponse() {
		return event.getInteractionResponse();
	}

	public void acknowledge() {
		if (!acknowledged) {
			acknowledged = true;
			event.deferReply().subscribe();
		}
	}

	public void acknowledgeEphemeral() {
		if (!acknowledged) {
			acknowledged = true;
			event.deferReply().withEphemeral(true).subscribe();
		}
	}

	public Optional<MessageData> respond(MessageBuilder messageBuilder) {
		if (!acknowledged) {
			acknowledgeEphemeral();
		}

		try {
			return Optional.ofNullable(getResponse().createFollowupMessage(messageBuilder.toMultipartWebhookExecuteRequest()).block());
		} catch (ClientException ex) {
			App.error("Failed to respond to slash command " + this + ": " + ex.getMessage());

			for (Throwable t : ex.getSuppressed()) {
				t.printStackTrace();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return Optional.empty();
	}

	public Optional<MessageData> respond(String content) {
		return respond(MessageBuilder.create(content));
	}

	public Optional<MessageData> respond(List<String> content) {
		return respond(String.join("\n", content));
	}

	public Optional<MessageData> respond(EmbedBuilder embed) {
		return respond(MessageBuilder.create(embed));
	}

	public void editInitial(MessageBuilder builder) {
		if (!acknowledged) {
			acknowledgeEphemeral();
		}

		getResponse().editInitialResponse(builder.toWebhookMessageEditRequest()).block();
	}

	public void editInitial(String content) {
		editInitial(MessageBuilder.create(content));
	}

	public void presentModal(String customId, String title, Collection<LayoutComponent> components) {
		event.presentModal(title, customId, components).block();
	}
}