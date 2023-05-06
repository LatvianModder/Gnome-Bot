package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.legacycommand.CommandContext;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.apps.webutils.FormattingUtils;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.LayoutComponent;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.interaction.InteractionResponse;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class DeferrableInteractionEventWrapper<T extends DeferrableInteractionEvent> extends InteractionEventWrapper<T> {
	protected boolean acknowledged;
	protected boolean edit = false;

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

	@Override
	protected CommandContext createContext() {
		/*
		return new CommandContext() {
			@Override
			public Message reply(String content) {
				respond(content);
				return null;
			}

			@Override
			public Message reply(Consumer<EmbedCreateSpec.Builder> espec) {
				respond(builder -> {
					EmbedCreateSpec.Builder ebuilder = EmbedCreateSpec.builder();
					ebuilder.color(EmbedColors.GRAY);
					espec.accept(ebuilder);
					builder.addEmbed(ebuilder.build());
				});
				return null;
			}
		};
		*/

		return super.createContext();
	}

	public DeferrableInteractionEventWrapper<T> edit() {
		edit = true;
		return this;
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
			if (edit) {
				return Optional.ofNullable(getResponse().editInitialResponse(messageBuilder.toMultipartWebhookMessageEditRequest()).block());
			} else {
				return Optional.ofNullable(getResponse().createFollowupMessage(messageBuilder.toMultipartWebhookExecuteRequest()).block());
			}
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

	public void respondModal(String customId, String title, Collection<LayoutComponent> components) {
		if (customId.length() > 100) {
			throw new GnomeException("Invalid custom modal ID: `" + customId + "`");
		}

		event.presentModal(FormattingUtils.trim(title, 45), customId, components).block();
	}

	public void respondModal(String customId, String title, ActionComponent... textInputs) {
		respondModal(customId, title, Arrays.stream(textInputs).map(ActionRow::of).collect(Collectors.toList()));
	}

	public boolean requiresTextResponse() {
		return true;
	}
}