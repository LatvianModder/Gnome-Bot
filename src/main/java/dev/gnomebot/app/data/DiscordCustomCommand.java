package dev.gnomebot.app.data;

import dev.gnomebot.app.discord.legacycommand.CommandContext;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandImpl;
import dev.gnomebot.app.util.MapWrapper;

import java.util.Date;
import java.util.List;

/**
 * @author LatvianModder
 */
public class DiscordCustomCommand extends WrappedDocument<DiscordCustomCommand> {
	public DiscordCustomCommand(WrappedCollection<DiscordCustomCommand> c, MapWrapper d) {
		super(c, d);
	}

	public String getCommandName() {
		return document.getString("command_name");
	}

	public long getAuthor() {
		return document.getLong("author");
	}

	@Override
	public Date getDate() {
		return document.getDate("created");
	}

	public int getPermissionLevel() {
		return document.getInt("permission_level", 0);
	}

	public List<String> getCommandList() {
		return document.getList("command_list");
	}

	public void runCommand(CommandContext context, CommandReader reader) throws Exception {
		List<String> commandList = getCommandList();

		if (commandList.size() == 1 && commandList.get(0).equals("dummy")) {
			throw new DiscordCommandException("Unhandled dummy command!");
		}

		for (String s : commandList) {
			CommandReader r = new CommandReader(context.gc, s
					.replace("\\n", "\n")
					.replace("${user}", context.sender.getId().asString())
					.replace("${username}", context.sender.getUsername())
					.replace("${usertag}", context.sender.getTag())
					.replace("${channel}", context.channelInfo.id.asString())
					.replace("${message}", context.message.getId().asString())
			);

			DiscordCommandImpl command = DiscordCommandImpl.BOT_COMMAND_MAP.get(r.readString().orElse("").toLowerCase());

			if (command != null) {
				command.callback.run(context, r);
			} else {
				throw new DiscordCommandException("Bot command not found! `" + s + "`");
			}
		}
	}
}