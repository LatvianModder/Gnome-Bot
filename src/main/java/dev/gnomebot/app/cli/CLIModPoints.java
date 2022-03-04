package dev.gnomebot.app.cli;

import com.google.gson.JsonObject;
import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.command.RegisterCommand;
import dev.gnomebot.app.util.URLRequest;

public class CLIModPoints {
	@RegisterCommand
	public static final CLICommand COMMAND = CLICommand.make("mod_points")
			.description("Mod Points")
			.noAdmin()
			.run(CLIModPoints::run);

	private static void run(CLIEvent event) throws Exception {
		long id = event.reader.readLong().orElse(0L);
		JsonObject json = URLRequest.of("https://addons-ecs.forgesvc.net/api/v2/addon/" + id + "/").toJsonObject().block();
		double popScore = json.get("popularityScore").getAsDouble();
		double constantFloor = 279.286184211;
		double constantCeil = 127.894761862;
		String calc = json.get("name").getAsString() + ": " + (int) (popScore / constantFloor) + " - " + (int) (popScore / constantCeil) + " (Score: " + popScore + ")";

		App.info(event.sender.getTag() + ": " + calc);
		event.respond(calc);
	}
}
