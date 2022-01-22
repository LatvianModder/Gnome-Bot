package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.Utils;
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
public final class DiscordCommandImpl {
	public static final Map<String, DiscordCommandImpl> COMMAND_MAP = new LinkedHashMap<>();
	public static final Map<String, DiscordCommandImpl> BOT_COMMAND_MAP = new LinkedHashMap<>();
	public static final List<DiscordCommandImpl> COMMAND_LIST = new ArrayList<>();

	public static void find() {
		COMMAND_MAP.clear();
		COMMAND_LIST.clear();
		BOT_COMMAND_MAP.clear();

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

							DiscordCommandImpl impl = new DiscordCommandImpl(params.getValue("name").toString(), (CommandCallback) o);
							impl.help = (String) params.getValue("help");
							impl.arguments = (String) params.getValue("arguments");
							impl.aliases = (String[]) params.getValue("aliases");
							impl.permissionLevel = (AuthLevel) ((AnnotationEnumValue) params.getValue("permissionLevel")).loadClassAndReturnEnumValue();

							if (impl.permissionLevel == AuthLevel.BOT) {
								BOT_COMMAND_MAP.put(impl.name, impl);
							} else {
								COMMAND_LIST.add(impl);
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		COMMAND_LIST.sort(Comparator.comparing(o -> o.name));

		for (DiscordCommandImpl cmd : COMMAND_LIST) {
			for (String a : cmd.aliases) {
				COMMAND_MAP.put(a, cmd);
			}
		}

		for (DiscordCommandImpl cmd : COMMAND_LIST) {
			COMMAND_MAP.put(cmd.name, cmd);
		}

		App.info("Found " + COMMAND_MAP.size() + " prefix commands");
		App.info("Found " + BOT_COMMAND_MAP.size() + " bot commands");
	}

	public static void run(CommandContext context, CommandReader reader, String content, boolean ignorePermissions) throws Exception {
		String commandName = reader.readString().orElse("").toLowerCase();
		DiscordCommandImpl command = COMMAND_MAP.get(commandName);

		if (command == null) {
			throw new DiscordCommandException(DiscordCommandException.Type.NOT_FOUND, "Command not found!");
		} else if (!ignorePermissions && !command.callback.hasPermission(command, context)) {
			throw new DiscordCommandException(DiscordCommandException.Type.NO_PERMISSION, "You don't have permission to use this command!").reaction(Emojis.POLICE_CAR);
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

	public DiscordCommandImpl(String n, CommandCallback c) {
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
		LinkedHashMap<String, Object> string = new LinkedHashMap<>();
		string.put("name", name);
		string.put("help", help);
		string.put("arguments", arguments);
		string.put("aliases", Arrays.asList(aliases));
		string.put("permissionLevel", permissionLevel.name());
		return Utils.toAnsiString(string);
	}

	public boolean hasPermission(CommandContext context) {
		if (permissionLevel == AuthLevel.NO_AUTH || permissionLevel == AuthLevel.LOGGED_IN) {
			return true;
		} else if (permissionLevel == AuthLevel.MEMBER) {
			return true;
		} else if (permissionLevel == AuthLevel.BOT) {
			return context.sender.getId().equals(context.handler.selfId);
		} else if (permissionLevel == AuthLevel.OWNER) {
			return context.sender.getId().equals(context.handler.selfId) || (context.channelInfo != null && context.channelInfo.getPermissions(context.sender.getId()).contains(Permission.ADMINISTRATOR));
		}

		return context.sender != null && context.gc.adminRole.is(context.sender);
	}
}