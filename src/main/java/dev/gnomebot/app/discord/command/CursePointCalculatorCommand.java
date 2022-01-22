package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.Currency;
import dev.gnomebot.app.util.Table;

/**
 * @author LatvianModder
 */
public class CursePointCalculatorCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("curse_point_calculator")
			.description("Curse Point calculator")
			.add(number("daily_points").required())
			.add(currency("currency"))
			.run(CursePointCalculatorCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		Currency c = event.get("currency").asCurrency().orElse(Currency.USD);
		double points = Math.min(Math.max(0D, event.get("daily_points").asDouble(0D)), 100000D);
		double money = points * 0.05D * c.rate;

		Table table = new Table("", "Day", "Month", "Year");
		table.addRow("Points", String.format("%.2f", points), String.format("%.2f", points * 30D), String.format("%.2f", points * 365D));
		table.addRow(c.name, String.format("%.2f", money), String.format("%.2f", money * 30D), String.format("%.2f", money * 365D));
		event.respond("```\n" + String.join("\n", table.getLines(false)) + "```");
	}
}
