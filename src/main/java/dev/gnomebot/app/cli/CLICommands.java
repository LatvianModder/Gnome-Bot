package dev.gnomebot.app.cli;

import dev.gnomebot.app.App;

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

		add(CLIAddGuildCommand.COMMAND);
		add(CLIAdvancedLogging.COMMAND);
		add(CLIAdvancedLoggingAll.COMMAND);
		add(CLIAuthLevel.COMMAND);
		add(CLIBans.COMMAND);
		add(CLIBrain.COMMAND);
		add(CLIChangeAvatar.COMMAND);
		add(CLICleanupDB.COMMAND);
		add(CLICountPresences.COMMAND);
		add(CLIDM.COMMAND);
		add(CLIEmojiLeaderboardCommand.COMMAND);
		add(CLIExportMessages.COMMAND);
		add(CLIFindSlurs.COMMAND);
		add(CLIFonts.COMMAND);
		add(CLIGuildIcon.COMMAND);
		add(CLIGuilds.COMMAND);
		add(CLIHeapdump.COMMAND);
		add(CLIIsolateConversation.COMMAND);
		add(CLIKickNewAccounts.COMMAND);
		add(CLIMemberCountPrintout.COMMAND);
		add(CLIModalTest.COMMAND);
		add(CLIPrintDMs.COMMAND);
		add(CLIRefreshRoles.COMMAND);
		add(CLIRegexFind.COMMAND);
		add(CLIRemoveReaction.COMMAND);
		add(CLIRestart.COMMAND);
		add(CLIRoleMentionChart.COMMAND);
		add(CLISlowResponseTest.COMMAND);
		add(CLITestAnsi.COMMAND);

		App.info("Found " + COMMANDS.size() + " CLI commands");
	}
}
