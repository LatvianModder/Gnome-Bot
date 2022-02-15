package dev.gnomebot.app.util;

import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.MessageComponent;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.MessageCreateFields;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.FollowupMessageRequest;
import discord4j.discordjson.json.ImmutableFollowupMessageRequest;
import discord4j.discordjson.json.ImmutableWebhookMessageEditRequest;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.discordjson.json.WebhookMessageEditRequest;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.MultipartRequest;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MessageBuilder {
	public static final AllowedMentions NO_MENTIONS = AllowedMentions.builder().build();

	public static MessageBuilder create() {
		return new MessageBuilder();
	}

	public static MessageBuilder create(String content) {
		return create().content(content);
	}

	public static MessageBuilder create(EmbedBuilder spec) {
		return create().addEmbed(spec);
	}

	private String content;
	private Boolean ephemeral;
	private List<EmbedBuilder> embeds;
	private AllowedMentions allowedMentions;
	private List<LayoutComponent> components;
	private List<MessageCreateFields.File> files;
	private Snowflake messageReference;

	private MessageBuilder() {
		content = null;
		ephemeral = null;
		embeds = null;
		allowedMentions = NO_MENTIONS;
		components = null;
		files = null;
		messageReference = null;
	}

	@Nullable
	public String getContent() {
		return this.content;
	}

	public MessageBuilder content(String content) {
		this.content = content;
		return this;
	}

	public MessageBuilder content(List<String> content) {
		return content(String.join("\n", content));
	}

	public MessageBuilder ephemeral(boolean ephemeral) {
		this.ephemeral = ephemeral;
		return this;
	}

	public MessageBuilder embeds(List<EmbedBuilder> embeds) {
		this.embeds = embeds;
		return this;
	}

	public MessageBuilder noEmbeds() {
		return embeds(Collections.emptyList());
	}

	public MessageBuilder addEmbed(EmbedBuilder embed) {
		if (this.embeds == null) {
			this.embeds = new ArrayList<>();
		}

		this.embeds.add(embed);
		return this;
	}

	public MessageBuilder allowedMentions(AllowedMentions allowedMentions) {
		this.allowedMentions = allowedMentions;
		return this;
	}

	public MessageBuilder components(List<LayoutComponent> components) {
		this.components = components;
		return this;
	}

	public MessageBuilder noComponents() {
		return components(Collections.emptyList());
	}

	public MessageBuilder addComponent(LayoutComponent component) {
		if (this.components == null) {
			this.components = new ArrayList<>();
		}

		this.components.add(component);
		return this;
	}

	public MessageBuilder addComponentRow(ActionComponent... components) {
		return addComponent(ActionRow.of(components));
	}

	public MessageBuilder addFile(String name, InputStream file) {
		if (this.files == null) {
			this.files = new ArrayList<>();
		}

		this.files.add(MessageCreateFields.File.of(name, file));
		return this;
	}

	public MessageBuilder addFile(String name, byte[] fileBytes) {
		return addFile(name, new ByteArrayInputStream(fileBytes));
	}

	public MessageBuilder messageReference(Snowflake messageReference) {
		this.messageReference = messageReference;
		return this;
	}

	// Specs //

	public MessageCreateSpec.Builder toMessageCreateSpec() {
		MessageCreateSpec.Builder builder = MessageCreateSpec.builder();

		if (this.content != null && !this.content.isEmpty()) {
			builder.content(Utils.trimContent(this.content));
		}

		if (this.embeds != null && !this.embeds.isEmpty()) {
			builder.embeds(this.embeds.stream().map(EmbedBuilder::toEmbedCreateSpec).toList());
		}

		builder.allowedMentions(this.allowedMentions);

		if (this.components != null && !this.components.isEmpty()) {
			builder.components(this.components);
		}

		if (this.files != null && !this.files.isEmpty()) {
			builder.files(this.files);
		}

		return builder;
	}

	public MultipartRequest<MessageCreateRequest> toMultipartMessageCreateRequest() {
		return toMessageCreateSpec().build().asRequest();
	}

	public MessageEditSpec.Builder toMessageEditSpec() {
		MessageEditSpec.Builder builder = MessageEditSpec.builder();

		if (this.content != null) {
			builder.contentOrNull(this.content.isEmpty() ? null : Utils.trimContent(this.content));
		}

		if (this.embeds != null) {
			builder.embedsOrNull(this.embeds.isEmpty() ? null : this.embeds.stream().map(EmbedBuilder::toEmbedCreateSpec).toList());
		}

		builder.allowedMentionsOrNull(this.allowedMentions);

		if (this.components != null) {
			builder.componentsOrNull(this.components.isEmpty() ? null : this.components);
		}

		if (this.files != null) {
			builder.files(this.files);
		}

		return builder;
	}

	public FollowupMessageRequest toFollowupMessageRequest() {
		ImmutableFollowupMessageRequest.Builder builder = FollowupMessageRequest.builder();

		if (this.content != null) {
			builder.content(Utils.trimContent(this.content));
		}

		if (this.embeds != null) {
			builder.embeds(this.embeds.stream().map(EmbedBuilder::toEmbedData).toList());
		}

		builder.allowedMentions(this.allowedMentions.toData());

		if (this.components != null) {
			builder.components(this.components.stream().map(MessageComponent::getData).collect(Collectors.toList()));
		}

		return builder.build();
	}

	public WebhookMessageEditRequest toWebhookMessageEditRequest() {
		ImmutableWebhookMessageEditRequest.Builder builder = WebhookMessageEditRequest.builder();

		if (this.content != null) {
			builder.contentOrNull(this.content.isEmpty() ? null : Utils.trimContent(this.content));
		}

		if (this.embeds != null) {
			builder.embedsOrNull(this.embeds.isEmpty() ? null : this.embeds.stream().map(EmbedBuilder::toEmbedData).toList());
		}

		builder.allowedMentionsOrNull(this.allowedMentions.toData());

		if (this.components != null) {
			builder.components(this.components.isEmpty() ? Possible.absent() : Possible.of(this.components.stream().map(LayoutComponent::getData).toList()));
		}

		return builder.build();
	}

	public MultipartRequest<? extends WebhookExecuteRequest> toMultipartWebhookExecuteRequest() {
		FollowupMessageRequest req = toFollowupMessageRequest();
		return files == null || files.isEmpty() ? MultipartRequest.ofRequest(req) : MultipartRequest.ofRequestAndFiles(req, files.stream().map(MessageCreateFields.File::asRequest).toList());
	}

	public InteractionApplicationCommandCallbackSpec toInteractionApplicationCommandCallbackSpec() {
		InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();

		if (this.content != null) {
			builder.content(Utils.trimContent(this.content));
		}

		if (this.embeds != null) {
			builder.embeds(this.embeds.stream().map(EmbedBuilder::toEmbedCreateSpec).toList());
		}

		builder.allowedMentions(this.allowedMentions);

		if (this.components != null) {
			builder.components(this.components);
		}

		builder.ephemeral(this.ephemeral == null || this.ephemeral);
		return builder.build();
	}
}
