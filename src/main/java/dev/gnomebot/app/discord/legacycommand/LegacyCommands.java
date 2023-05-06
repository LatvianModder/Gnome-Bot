package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.server.AuthLevel;
import dev.latvian.apps.webutils.ansi.AnsiJava;
import discord4j.rest.util.Permission;
import io.github.classgraph.AnnotationEnumValue;
import io.github.classgraph.AnnotationParameterValueList;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.ScanResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LatvianModder
 */
public final class LegacyCommands {
	public static final Map<String, LegacyCommands> COMMAND_MAP = new LinkedHashMap<>();
	public static final List<LegacyCommands> COMMAND_LIST = new ArrayList<>();

	public static void find() {
		COMMAND_MAP.clear();
		COMMAND_LIST.clear();

		String pkg = LegacyDiscordCommand.class.getPackage().getName();
		String commandAnnotation = LegacyDiscordCommand.class.getName();
		// enableAllInfo()
		try (ScanResult scanResult = new ClassGraph().enableFieldInfo().enableAnnotationInfo().acceptPackages(pkg).scan()) {
			for (ClassInfo commandClassInfo : scanResult.getClassesWithFieldAnnotation(commandAnnotation)) {
				for (FieldInfo fieldInfo : commandClassInfo.getFieldInfo()) {
					if (fieldInfo.hasAnnotation(commandAnnotation)) {
						Object o = fieldInfo.loadClassAndGetField().get(null);

						if (o instanceof CommandCallback) {
							AnnotationParameterValueList params = fieldInfo.getAnnotationInfo(commandAnnotation).getParameterValues();

							LegacyCommands impl = new LegacyCommands(params.getValue("name").toString(), (CommandCallback) o);
							impl.help = (String) params.getValue("help");
							impl.arguments = (String) params.getValue("arguments");
							impl.aliases = (String[]) params.getValue("aliases");
							impl.permissionLevel = (AuthLevel) ((AnnotationEnumValue) params.getValue("permissionLevel")).loadClassAndReturnEnumValue();
							COMMAND_LIST.add(impl);
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		COMMAND_LIST.sort(Comparator.comparing(o -> o.name));

		for (LegacyCommands cmd : COMMAND_LIST) {
			for (String a : cmd.aliases) {
				COMMAND_MAP.put(a, cmd);
			}
		}

		for (LegacyCommands cmd : COMMAND_LIST) {
			COMMAND_MAP.put(cmd.name, cmd);
		}

		App.info("Found " + COMMAND_MAP.size() + " legacy commands");
	}

	public static void run(CommandContext context, CommandReader reader, String content, boolean ignorePermissions) throws Exception {
		String commandName = reader.readString().orElse("").toLowerCase();
		LegacyCommands command = COMMAND_MAP.get(commandName);

		if (command == null) {
			throw new GnomeException(GnomeException.Type.NOT_FOUND, "Command not found!");
		} else if (!ignorePermissions && !command.callback.hasPermission(command, context)) {
			throw new GnomeException(GnomeException.Type.NO_PERMISSION, "You don't have permission to use this command!").reaction(Emojis.POLICE_CAR);
		} else {
			context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.COMMAND)
					.channel(context.channelInfo.id)
					.message(context.message)
					.user(context.sender)
					.oldContent(command.name)
					.content(content)
			);

			command.callback.run(context, reader);
		}
	}

	public final String name;
	public final CommandCallback callback;
	public String help;
	public String arguments;
	public String[] aliases;
	public AuthLevel permissionLevel;

	public LegacyCommands(String n, CommandCallback c) {
		name = n;
		callback = c;
		help = "No information";
		arguments = "";
		aliases = new String[0];
		permissionLevel = AuthLevel.MEMBER;
	}

	public String getInfo() {
		return arguments.isEmpty() ? name : (name + " " + arguments);
	}

	@Override
	public String toString() {
		var map = new LinkedHashMap<String, Object>();
		map.put("name", name);
		map.put("help", help);
		map.put("arguments", arguments);
		map.put("aliases", Arrays.asList(aliases));
		map.put("permissionLevel", permissionLevel.name());
		return AnsiJava.of(map).toAnsiString();
	}

	public boolean hasPermission(CommandContext context) {
		if (permissionLevel == AuthLevel.NO_AUTH || permissionLevel == AuthLevel.LOGGED_IN) {
			return true;
		} else if (permissionLevel == AuthLevel.MEMBER) {
			return true;
		} else if (permissionLevel == AuthLevel.OWNER) {
			return context.sender.getId().equals(context.handler.selfId) || (context.channelInfo != null && context.channelInfo.checkPermissions(context.sender.getId(), Permission.ADMINISTRATOR));
		}

		return context.sender != null && context.gc.adminRole.is(context.sender);
	}
}