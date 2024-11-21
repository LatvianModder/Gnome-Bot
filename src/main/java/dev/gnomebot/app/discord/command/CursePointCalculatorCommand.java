package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.Currency;
import dev.latvian.apps.ansi.ANSI;
import dev.latvian.apps.ansi.ANSITable;

public class CursePointCalculatorCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("curse-point-calculator")
			.supportsDM()
			.description("Curse Point calculator")
			.add(number("daily-points").required())
			.add(currency("currency"))
			.run(CursePointCalculatorCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		var c = event.get("currency").asCurrency().orElse(Currency.USD);
		var points = Math.min(Math.max(0D, event.get("daily-points").asDouble(0D)), 100000D);
		var money = points * 0.05D * c.rate;

		var table = new ANSITable("", "Day", "Month", "Year");
		table.addRow("Points", String.format("%.2f", points), String.format("%.2f", points * 30D), String.format("%.2f", points * 365D));
		table.addRow(c.name, String.format("%.2f", money), String.format("%.2f", money * 30D), String.format("%.2f", money * 365D));
		event.respond("```ansi\n" + ANSI.join(ANSI.LINE, table.getLines()) + "\n```");
	}
}
