package dev.gnomebot.app.util;

import dev.gnomebot.app.App;
import dev.latvian.apps.webutils.FormattingUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.MessageComponent;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.TextInput;
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
import java.util.Map;
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

	public static MessageBuilder of(Object object) {
		if (object == null) {
			return null;
		} else if (object instanceof CharSequence) {
			return create(object.toString());
		} else if (object instanceof Map<?, ?> map) {
			MessageBuilder builder = MessageBuilder.create();

			if (map.get("content") instanceof CharSequence s) {
				builder.content(s.toString());
			}

			if (map.get("embed") instanceof Map m) {
				builder.addEmbed(EmbedBuilder.of(m));
			}

			if (map.get("embeds") instanceof Iterable itr) {
				for (var o : itr) {
					if (o instanceof Map m) {
						builder.addEmbed(EmbedBuilder.of(m));
					}
				}
			}

			return builder;
		}

		App.error("Invalid script message: " + object);
		return MessageBuilder.create("Invalid script message!");
	}

	private String content;
	private Boolean ephemeral;
	private List<EmbedBuilder> embeds;
	private AllowedMentions allowedMentions;
	private List<LayoutComponent> components;
	private List<MessageCreateFields.File> files;
	private Snowflake messageReference;
	private String webhookName;
	private String webhookAvatarUrl;

	private MessageBuilder() {
		content = null;
		ephemeral = null;
		embeds = null;
		allowedMentions = NO_MENTIONS;
		components = null;
		files = null;
		messageReference = null;
		webhookName = null;
		webhookAvatarUrl = null;
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

	public MessageBuilder allowUserMentions(Snowflake... ids) {
		return allowedMentions(AllowedMentions.builder().allowUser(ids).build());
	}

	public MessageBuilder allowRoleMentions(Snowflake... ids) {
		return allowedMentions(AllowedMentions.builder().allowRole(ids).build());
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

	public MessageBuilder dynamicComponents(Iterable<ActionComponent> components) {
		ActionComponent last = null;
		var row = new ArrayList<ActionComponent>();

		for (var component : components) {
			if (!row.isEmpty() && (row.size() >= 5 || last instanceof SelectMenu || last instanceof TextInput)) {
				addComponent(ActionRow.of(row));
				row = new ArrayList<>();
			}

			row.add(component);
			last = component;
		}

		if (!row.isEmpty()) {
			addComponent(ActionRow.of(row));
		}

		return this;
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

	public MessageBuilder noFiles() {
		this.files = new ArrayList<>();
		return this;
	}

	public MessageBuilder messageReference(Snowflake messageReference) {
		this.messageReference = messageReference;
		return this;
	}

	public MessageBuilder webhookName(String webhookName) {
		this.webhookName = webhookName;
		return this;
	}

	public MessageBuilder webhookAvatarUrl(String webhookAvatarUrl) {
		this.webhookAvatarUrl = webhookAvatarUrl;
		return this;
	}

	// Specs //

	public MessageCreateSpec toMessageCreateSpec() {
		MessageCreateSpec.Builder builder = MessageCreateSpec.builder();

		if (this.content != null && !this.content.isEmpty()) {
			builder.content(FormattingUtils.trimContent(this.content));
		}

		if (this.embeds != null && !this.embeds.isEmpty()) {
			builder.embeds(this.embeds.stream().map(EmbedBuilder::toEmbedCreateSpec).toList());
		}

		builder.allowedMentions(this.allowedMentions);

		if (this.components != null && !this.components.isEmpty()) {
			builder.components(this.components);
		}

		if (this.files != null) {
			builder.files(this.files);
		}

		if (this.messageReference != null) {
			builder.messageReference(this.messageReference);
		}

		return builder.build();
	}

	public MultipartRequest<MessageCreateRequest> toMultipartMessageCreateRequest() {
		return toMessageCreateSpec().asRequest();
	}

	public MessageEditSpec.Builder toMessageEditSpec() {
		MessageEditSpec.Builder builder = MessageEditSpec.builder();

		if (this.content != null) {
			builder.contentOrNull(this.content.isEmpty() ? null : FormattingUtils.trimContent(this.content));
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
			builder.content(FormattingUtils.trimContent(this.content));
		}

		if (this.embeds != null) {
			builder.embeds(this.embeds.stream().map(EmbedBuilder::toEmbedData).toList());
		}

		builder.allowedMentions(this.allowedMentions.toData());

		if (this.components != null) {
			builder.components(this.components.stream().map(MessageComponent::getData).collect(Collectors.toList()));
		}

		if (this.webhookName != null) {
			builder.username(this.webhookName);
		}

		if (this.webhookAvatarUrl != null) {
			builder.avatarUrl(this.webhookAvatarUrl);
		}

		return builder.build();
	}

	public MultipartRequest<? extends WebhookExecuteRequest> toMultipartWebhookExecuteRequest() {
		var req = toFollowupMessageRequest();
		return files == null || files.isEmpty() ? MultipartRequest.ofRequest(req) : MultipartRequest.ofRequestAndFiles(req, files.stream().map(MessageCreateFields.File::asRequest).toList());
	}

	public WebhookMessageEditRequest toWebhookMessageEditRequest() {
		ImmutableWebhookMessageEditRequest.Builder builder = WebhookMessageEditRequest.builder();

		if (this.content != null) {
			builder.contentOrNull(this.content.isEmpty() ? null : FormattingUtils.trimContent(this.content));
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

	public MultipartRequest<WebhookMessageEditRequest> toMultipartWebhookMessageEditRequest() {
		var req = toWebhookMessageEditRequest();
		return files == null || files.isEmpty() ? MultipartRequest.ofRequest(req) : MultipartRequest.ofRequestAndFiles(req, files.stream().map(MessageCreateFields.File::asRequest).toList());
	}

	public InteractionApplicationCommandCallbackSpec toInteractionApplicationCommandCallbackSpec() {
		InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();

		if (this.content != null) {
			builder.content(FormattingUtils.trimContent(this.content));
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
