package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.command.CommandOption;
import dev.gnomebot.app.discord.legacycommand.CommandContext;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.MessageComponent;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.TextInput;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class ModalEventWrapper extends ComponentEventWrapper {
	public final Map<String, CommandOption> options;
	public final Map<String, List<String>> selectMenuOptions;

	public ModalEventWrapper(GuildCollections gc, ModalSubmitInteractionEvent e, String id) {
		super(gc, e, id);
		options = new HashMap<>();
		selectMenuOptions = new HashMap<>();
		mapOptions(context, e.getComponents(), options, selectMenuOptions);
	}

	private static void mapOptions(CommandContext context, List<MessageComponent> list, Map<String, CommandOption> options, Map<String, List<String>> selectMenuOptions) {
		for (MessageComponent component : list) {
			if (component instanceof ActionRow row) {
				mapOptions(context, row.getChildren(), options, selectMenuOptions);
			} else if (component instanceof TextInput textInput) {
				CommandOption o1 = new CommandOption(context, textInput.getCustomId(), textInput.getValue().orElse(""), false);
				options.put(o1.name, o1);
			} else if (component instanceof SelectMenu selectMenu) {
				selectMenuOptions.put(selectMenu.getCustomId(), selectMenu.getOptions().stream().map(SelectMenu.Option::getValue).toList());
			}
		}
	}

	@Override
	public String toString() {
		return super.toString() + " " + options + " " + selectMenuOptions;
	}

	public boolean has(String id) {
		return options.containsKey(id);
	}

	public CommandOption get(String id) throws DiscordCommandException {
		CommandOption o = options.get(id);

		if (o == null) {
			return new CommandOption(context, id, "", false);
		}

		return o;
	}
}
