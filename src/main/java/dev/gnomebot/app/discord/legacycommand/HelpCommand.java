package dev.gnomebot.app.discord.legacycommand;

import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class HelpCommand {
	@LegacyDiscordCommand(name = "help", help = "Prints help in DM", aliases = "commands")
	public static final CommandCallback COMMAND = (context, reader) -> {
		String s = reader.readString().orElse("");

		if (s.isEmpty()) {
			StringBuilder sb = new StringBuilder("`");
			sb.append(context.gc.prefix);
			sb.append("commands`:\n");
			sb.append(DiscordCommandImpl.COMMAND_LIST.stream().filter(c -> c.callback.hasPermission(c, context)).map(c -> "`" + c.name + "`").collect(Collectors.joining(", ")));

			long macros = context.gc.macros.count();

			if (macros > 0L) {
				sb.append("\n\n`");
				sb.append(context.gc.macroPrefix);
				sb.append("macros`: ");
				sb.append(macros);
			}

			sb.append("\n\nTo configure or add this bot to your server, visit [gnomebot.dev](https://gnomebot.dev/)");
			context.reply("Command List", sb.toString());
			return;
		}

		DiscordCommandImpl cmd = DiscordCommandImpl.COMMAND_MAP.get(s);

		if (cmd == null) {
			throw new DiscordCommandException("Command not found!");
		} else if (!cmd.callback.hasPermission(cmd, context)) {
			throw new DiscordCommandException("You don't have permission to use this command!");
		}

		StringBuilder sb = new StringBuilder();
		sb.append('`');
		sb.append(context.gc.prefix);
		sb.append(s);

		if (!cmd.arguments.isEmpty()) {
			sb.append(' ');
			sb.append(cmd.arguments);
		}

		sb.append("` - ");
		sb.append(cmd.help);

		context.reply("Command Info", sb.toString());
	};
}
