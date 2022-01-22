package dev.gnomebot.app.discord.command;

import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.discordjson.possible.Possible;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CommandBuilder {
	private static final Pattern COMMAND_NAME_PATTERN = Pattern.compile("^[\\w-\\s]{1,32}$");

	public CommandBuilder parent;
	public final ApplicationCommandOption.Type type;
	public final String name;
	private int applicationType;
	private String description;
	private Possible<Boolean> required;
	private List<ApplicationCommandOptionChoiceData> choices;
	private LinkedHashMap<String, CommandBuilder> options;
	public SlashCommandCallback callback;
	public AutoCompleteSuggestionCallback suggestions;
	private final boolean defaultPermission;

	public CommandBuilder(ApplicationCommandOption.Type t, String n) {
		type = t;
		name = n;

		if (!COMMAND_NAME_PATTERN.matcher(name).matches()) {
			throw new RuntimeException("Invalid command name: " + name);
		}

		applicationType = 1;
		description = name.substring(0, 1).toUpperCase() + name.substring(1).replace('_', ' ');
		required = Possible.absent();
		choices = null;
		options = null;
		callback = SlashCommandCallback.DEFAULT;
		suggestions = null;
		defaultPermission = true;
	}

	@Override
	public String toString() {
		return parent == null ? name : (parent + " " + name);
	}

	public CommandBuilder add(CommandBuilder builder) {
		if (options == null) {
			options = new LinkedHashMap<>();
		}

		builder.parent = this;
		options.put(builder.name, builder);
		return this;
	}

	public CommandBuilder description(String s) {
		description = s;
		return this;
	}

	public CommandBuilder required() {
		required = Possible.of(true);
		return this;
	}

	public CommandBuilder notRequired() {
		required = Possible.of(false);
		return this;
	}

	public CommandBuilder choice(ApplicationCommandOptionChoiceData choice) {
		if (choices == null) {
			choices = new ArrayList<>();
		}

		choices.add(choice);
		return this;
	}

	public CommandBuilder choice(String name, Object value) {
		return choice(ApplicationCommandOptionChoiceData.builder().name(name).value(value).build());
	}

	public CommandBuilder choices(Stream<ApplicationCommandOptionChoiceData> choices) {
		choices.forEachOrdered(this::choice);
		return this;
	}

	public CommandBuilder run(SlashCommandCallback c) {
		callback = c;
		return this;
	}

	public CommandBuilder suggest(AutoCompleteSuggestionCallback c) {
		suggestions = c;
		return this;
	}

	public CommandBuilder requiresPermission() {
		// defaultPermission = false;
		return this;
	}

	public List<ApplicationCommandOptionData> createOptions() {
		if (options == null) {
			return Collections.emptyList();
		}

		List<ApplicationCommandOptionData> list = new ArrayList<>();

		for (CommandBuilder builder : options.values()) {
			ImmutableApplicationCommandOptionData.Builder b = ApplicationCommandOptionData.builder();
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

	public CommandBuilder userInteraction() {
		applicationType = 2;
		return this;
	}

	public CommandBuilder messageInteraction() {
		applicationType = 3;
		return this;
	}

	public ImmutableApplicationCommandRequest createRootRequest() {
		ImmutableApplicationCommandRequest.Builder b = ApplicationCommandRequest.builder();
		b.type(applicationType);
		b.name(name);

		if (applicationType == 1) {
			b.description(description);
		}

		b.defaultPermission(defaultPermission);

		List<ApplicationCommandOptionData> options = createOptions();

		if (!options.isEmpty()) {
			b.options(options);
		}

		return b.build();
	}

	@Nullable
	public CommandBuilder getSub(String key) {
		return options.get(key);
	}
}
