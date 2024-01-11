package dev.gnomebot.app.cli;

import dev.latvian.apps.webutils.ansi.Table;

import java.time.Instant;

public class CLIMemberCountPrintout {
	public static final CLICommand COMMAND = CLICommand.make("membercount_printout")
			.description("Membercount Printout")
			.run(CLIMemberCountPrintout::run);

	private static void run(CLIEvent event) throws Exception {
		var role = event.reader.readRole().orElse(null);

		var table = new Table("ID", "Tag");

		for (var member : event.gc.getMembers()) {
			if (role == null || member.getRoleIds().contains(role.id)) {
				table.addRow(member.getId().asString(), member.getTag());
			}
		}

		event.respond("Done!");
		event.response.addFile("role-export-" + event.gc.guildId.asString() + "-" + (role == null ? "all" : role.id.asString()) + "-" + Instant.now().toString().replace(':', '-') + ".csv", table.getCSVBytes(false));
	}
}
