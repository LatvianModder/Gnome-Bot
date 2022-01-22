package dev.gnomebot.app.data;

import dev.gnomebot.app.discord.EmbedColors;
import dev.gnomebot.app.discord.QuoteHandler;
import dev.gnomebot.app.discord.legacycommand.CommandContext;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandImpl;
import dev.gnomebot.app.util.MapWrapper;
import discord4j.common.util.Snowflake;

import java.util.Date;
import java.util.List;

/**
 * @author LatvianModder
 */
public class DiscordCustomCommand extends WrappedDocument<DiscordCustomCommand> {
	// TODO: Hardcoded
	public static String mmRuleCache = null;

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
			if (context.gc.isMM()) {
				int rule = reader.readLong().orElse(0L).intValue();

				if (rule < 1) {
					throw new DiscordCommandException("What a funny guy. That rule doesn't exist...");
				}

				if (mmRuleCache == null) {
					mmRuleCache = context.handler.client.getMessageById(Snowflake.of(220243899191394304L), Snowflake.of(888091417103183902L)).block().getContent();
				}

				for (String s : mmRuleCache.split("\n")) {
					if (s.startsWith("    " + rule + ".")) {
						context.referenceMessage = false;
						context.reply(builder -> {
							builder.color(EmbedColors.RED);
							builder.title("Rule #" + rule);
							builder.url(QuoteHandler.getMessageURL(Snowflake.of(166630061217153024L), Snowflake.of(220243899191394304L), Snowflake.of(888091417103183902L)));
							builder.description(s.substring(s.indexOf('.') + 1).replace(". ", ".\n").trim());
						});

						return;
					}
				}

				throw new DiscordCommandException("What a funny guy. That rule doesn't exist...");
			}

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