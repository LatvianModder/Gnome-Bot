package dev.gnomebot.app.cli;

import dev.gnomebot.app.discord.command.CLIApplicationCommand;

public class CLISlowResponseTest {
	public static final CLICommand COMMAND = CLICommand.make("slow_response_test")
			.description("Slow response")
			.noAdmin()
			.run(CLISlowResponseTest::run);

	private static void run(CLIEvent event) {
		event.response = null;

		if (event instanceof CLIApplicationCommand.CLIEventFromCommand e) {
			new Thread(() -> {
				try {
					for (int i = 0; i < 10; i++) {
						e.event.edit().respond("Slow response: " + (10 - i));
						Thread.sleep(1000L);
					}

					e.event.edit().respond("Done!");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}).start();
		}

		// event.response.addComponentRow(Button.danger("modal_test", "Click this to open test form!"));
	}
}
