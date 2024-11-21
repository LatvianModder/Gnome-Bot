package dev.gnomebot.app.cli;

import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.ansi.ANSITable;

import java.time.Instant;

public class CLIMemberCountPrintout {
	public static final CLICommand COMMAND = CLICommand.make("membercount_printout")
			.description("Membercount Printout")
			.run(CLIMemberCountPrintout::run);

	private static void run(CLIEvent event) throws Exception {
		var role = event.reader.readRole().orElse(null);

		var table = new ANSITable("ID", "Tag");

		for (var member : event.gc.getMembers()) {
			if (role == null || member.getRoleIds().contains(SnowFlake.convert(role.id))) {
				table.addRow(member.getId().asString(), member.getTag());
			}
		}

		event.respond("Done!");
		event.response.addFile("role-export-" + event.gc.guildId + "-" + (role == null ? "all" : role.id) + "-" + Instant.now().toString().replace(':', '-') + ".csv", table.getCSVBytes(false));
	}
}
