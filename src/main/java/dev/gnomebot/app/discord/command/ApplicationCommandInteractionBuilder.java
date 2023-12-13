package dev.gnomebot.app.discord.command;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.PermissionSet;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ApplicationCommandInteractionBuilder<E extends ApplicationCommandInteractionEvent, W extends ApplicationCommandInteractionEventWrapper<E>, Self extends ApplicationCommandInteractionBuilder<E, W, Self>> {
	private static final Pattern COMMAND_NAME_PATTERN = Pattern.compile("^[\\w-\\s]{1,32}$");

	public Self parent;
	public final InteractionType<Self> interactionType;
	public final ApplicationCommandOption.Type type;
	public final String name;
	public UUID commandHash;
	protected String description;
	protected boolean required;
	protected List<ApplicationCommandOptionChoiceData> choices;
	protected LinkedHashMap<String, Self> options;
	public ApplicationCommandCallback<E, W> callback;
	public AutoCompleteSuggestionCallback suggestions;
	public boolean supportsDM;
	public PermissionSet defaultMemberPermissions;

	public ApplicationCommandInteractionBuilder(InteractionType<Self> it, ApplicationCommandOption.Type t, String n) {
		interactionType = it;
		type = t;
		name = n;

		if (!COMMAND_NAME_PATTERN.matcher(name).matches()) {
			throw new RuntimeException("Invalid command name: " + name);
		}

		commandHash = null;
		description = name.substring(0, 1).toUpperCase() + name.substring(1).replace('_', ' ');
		required = false;
		choices = null;
		options = null;
		suggestions = null;
		supportsDM = false;
		defaultMemberPermissions = null;
	}

	@Override
	public String toString() {
		return parent == null ? name : (parent + " " + name);
	}

	public Self self() {
		return (Self) this;
	}

	public Self add(Self builder) {
		if (options == null) {
			options = new LinkedHashMap<>();
		}

		builder.parent = self();
		options.put(builder.name, builder);
		return self();
	}

	public Self description(String s) {
		description = s;
		return self();
	}

	public Self required() {
		required = true;
		return self();
	}

	public Self choice(ApplicationCommandOptionChoiceData choice) {
		if (choices == null) {
			choices = new ArrayList<>();
		}

		choices.add(choice);
		return self();
	}

	public Self choice(String name, Object value) {
		return choice(ApplicationCommandOptionChoiceData.builder().name(name).value(value).build());
	}

	public Self choices(Stream<ApplicationCommandOptionChoiceData> choices) {
		choices.forEachOrdered(this::choice);
		return self();
	}

	public Self run(ApplicationCommandCallback<E, W> c) {
		callback = c;
		return self();
	}

	public Self suggest(AutoCompleteSuggestionCallback c) {
		suggestions = c;
		return self();
	}

	public Self supportsDM() {
		supportsDM = true;
		return self();
	}

	public Self defaultMemberPermissions(PermissionSet p) {
		defaultMemberPermissions = p;
		return self();
	}

	public List<ApplicationCommandOptionData> createOptions() {
		if (options == null) {
			return Collections.emptyList();
		}

		List<ApplicationCommandOptionData> list = new ArrayList<>();

		for (Self builder : options.values()) {
			var b = ApplicationCommandOptionData.builder();
			b.type(builder.type.getValue());
			b.name(builder.name);
			b.description(builder.description);
			b.required(builder.required);

			if (builder.suggestions != null) {
				b.autocomplete(true);
			}

			if (builder.choices != null) {
				b.choices(builder.choices);
			}

			List<ApplicationCommandOptionData> o = builder.createOptions();

			if (!o.isEmpty()) {
				b.options(o);
			}

			list.add(b.build());
		}

		return list;
	}

	public ApplicationCommandRequest createRootRequest() {
		var b = ApplicationCommandRequest.builder();
		b.type(interactionType.type);
		b.name(name);

		if (defaultMemberPermissions != null) {
			b.defaultMemberPermissions(Long.toUnsignedString(defaultMemberPermissions.getRawValue()));
		}

		if (interactionType.hasDescription) {
			b.description(description);
		}

		var options = createOptions();

		if (!options.isEmpty()) {
			b.options(options);
		}

		return b.build();
	}

	protected void writeHash(DataOutputStream stream) throws IOException {
		stream.writeByte(interactionType.type);
		stream.writeUTF(name);
		stream.writeUTF(description);
		stream.writeBoolean(required);
		stream.writeBoolean(suggestions != null);

		if (choices != null) {
			for (var choice : choices) {
				stream.writeUTF(choice.name());

				if (choice.value() instanceof Number) {
					stream.writeDouble(((Number) choice.value()).doubleValue());
				} else {
					stream.writeUTF(String.valueOf(choice.value()));
				}
			}
		}

		if (options != null) {
			for (var option : options.values()) {
				option.writeHash(stream);
			}
		}

		if (defaultMemberPermissions != null) {
			stream.writeLong(defaultMemberPermissions.getRawValue());
		}
	}

	public UUID createHash() {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (DataOutputStream stream = new DataOutputStream(bytes)) {
				writeHash(stream);
			}

			return UUID.nameUUIDFromBytes(bytes.toByteArray());
		} catch (Exception e) {
			return ApplicationCommands.INVALID_APPLICATION_COMMAND;
		}
	}

	@Nullable
	public Self getSub(String key) {
		return options.get(key);
	}
}
