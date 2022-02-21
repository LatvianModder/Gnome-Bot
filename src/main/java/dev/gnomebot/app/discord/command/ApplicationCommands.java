package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.Currency;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import discord4j.core.object.command.ApplicationCommandOption;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.ScanResult;

import java.util.HashMap;
import java.util.Map;

public class ApplicationCommands {
	public static final Map<String, CommandBuilder> COMMANDS = new HashMap<>();

	public static void find() {
		COMMANDS.clear();

		String pkg = RootCommand.class.getPackage().getName();
		String commandAnnotation = RootCommand.class.getName();
		//enableAllInfo()
		try (ScanResult scanResult = new ClassGraph().enableFieldInfo().enableAnnotationInfo().acceptPackages(pkg).scan()) {
			for (ClassInfo commandClassInfo : scanResult.getClassesWithFieldAnnotation(commandAnnotation)) {
				for (FieldInfo fieldInfo : commandClassInfo.getFieldInfo()) {
					if (fieldInfo.hasAnnotation(commandAnnotation)) {
						Object o = fieldInfo.loadClassAndGetField().get(null);

						if (o instanceof CommandBuilder c) {
							if (COMMANDS.containsKey(c.name)) {
								throw new RuntimeException("Slash Command already registered! " + c.name);
							}

							COMMANDS.put(c.name, c);
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		App.info("Found " + COMMANDS.size() + " slash commands");
	}

	public static GnomeException error(String message) {
		return new GnomeException(message);
	}

	public static GnomeException wip() {
		return error("WIP!");
	}

	public static CommandBuilder builder(ApplicationCommandOption.Type type, String name) {
		return new CommandBuilder(type, name);
	}

	public static CommandBuilder root(String name) {
		return builder(ApplicationCommandOption.Type.UNKNOWN, name);
	}

	public static CommandBuilder sub(String name) {
		return builder(ApplicationCommandOption.Type.SUB_COMMAND, name);
	}

	public static CommandBuilder subGroup(String name) {
		return builder(ApplicationCommandOption.Type.SUB_COMMAND_GROUP, name);
	}

	public static CommandBuilder basic(ApplicationCommandOption.Type type, String name) {
		return builder(type, name).notRequired();
	}

	public static CommandBuilder string(String name) {
		return basic(ApplicationCommandOption.Type.STRING, name);
	}

	public static CommandBuilder integer(String name) {
		return basic(ApplicationCommandOption.Type.INTEGER, name);
	}

	public static CommandBuilder bool(String id) {
		return basic(ApplicationCommandOption.Type.BOOLEAN, id);
	}

	public static CommandBuilder user(String id) {
		return basic(ApplicationCommandOption.Type.STRING, id).suggest(event -> event.context.gc.usernameSuggestions(event));
	}

	public static CommandBuilder channel(String id) {
		return basic(ApplicationCommandOption.Type.CHANNEL, id);
	}

	public static CommandBuilder role(String id) {
		return basic(ApplicationCommandOption.Type.ROLE, id);
	}

	public static CommandBuilder time(String id, boolean all) {
		return basic(ApplicationCommandOption.Type.STRING, id).suggest(event -> {
			if (all) {
				event.suggest("all");
			}

			event.suggest("30 seconds");
			event.suggest("1 minute");
			event.suggest("10 minutes");
			event.suggest("30 minutes");
			event.suggest("1 hour");
			event.suggest("3 hours");
			event.suggest("6 hours");
			event.suggest("1 day");
			event.suggest("1 week");
			event.suggest("1 month");
			event.suggest("3 months");
			event.suggest("1 year");
		});
	}

	public static CommandBuilder number(String id) {
		return basic(ApplicationCommandOption.Type.NUMBER, id);
	}

	public static CommandBuilder zone(String id) {
		return string(id);
	}

	public static CommandBuilder currency(String id) {
		return string(id).suggest(event -> {
			for (Currency currency : Currency.ALL.getNonnull().values()) {
				event.suggest(currency.name, currency.id);
			}
		});
	}
}
