package dev.gnomebot.app.cli;

import dev.latvian.apps.ansi.log.Log;

import java.util.HashMap;
import java.util.Map;

public class CLICommands {
	public static final Map<String, CLICommand> COMMANDS = new HashMap<>();

	public static void add(CLICommand c) {
		if (COMMANDS.containsKey(c.name)) {
			throw new RuntimeException("CLI Command already registered! " + c.name);
		}

		COMMANDS.put(c.name, c);
	}

	public static void find() {
		COMMANDS.clear();

		add(CLIAdvancedLogging.COMMAND);
		add(CLIAdvancedLoggingAll.COMMAND);
		add(CLIDM.COMMAND);
		add(CLIEmojiLeaderboardCommand.COMMAND);
		add(CLIMemberCountPrintout.COMMAND);
		add(CLIRefreshRoles.COMMAND);
		add(CLIRestart.COMMAND);
		add(CLIRoleMentionChart.COMMAND);

		Log.info("Found " + COMMANDS.size() + " CLI commands");
	}
}
