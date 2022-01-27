package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.ScamHandler;

/**
 * @author LatvianModder
 */
public class ScamsCommands extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("scams")
			.description("Commands to manage scam URL detection")
			.add(subGroup("domains")
					.description("Manage domains")
					.add(sub("fetch")
							.description("Fetches list from SinkingYachts")
							.run(ScamsCommands::fetchDomains)
					)
					.add(sub("check")
							.description("Checks if domain is blocked or allowed")
							.add(string("domain").required())
							.run(ScamsCommands::checkDomain)
					)
					.add(sub("block")
							.description("Blocks domain (For scam URLs)")
							.add(string("domain").required())
							.run(ScamsCommands::blockDomain)
					)
					.add(sub("allow")
							.description("Allows domain (For false positives)")
							.add(string("domain").required())
							.run(ScamsCommands::allowDomain)
					)
					.add(sub("remove")
							.description("Removes domain from being either blocked or allowed")
							.add(string("domain").required())
							.run(ScamsCommands::removeDomain)
					)
			)
			.add(sub("test")
					.description("Tests and prints back if input contains any scam URLs")
					.add(string("text").required())
					.run(ScamsCommands::test)
			);

	private static void fetchDomains(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderTrusted();
		ScamHandler.fetchDomains(() -> event.respond("Done!"));
	}

	private static void checkDomain(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		String domain = event.get("domain").asString();

		if (ScamHandler.EXACT_DOMAIN.matcher(domain).matches()) {
			event.respond("Domain type: " + ScamHandler.checkScamDomain(domain).commandName);
		} else {
			event.respond("Please use only domain, excluding https and path!");
		}
	}

	private static void blockDomain(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		String domain = event.get("domain").asString();

		if (ScamHandler.EXACT_DOMAIN.matcher(domain).matches()) {
			ScamHandler.setOverride(event.context.gc, event.context.sender.getTag(), domain, ScamHandler.Type.BLOCK);
			event.respond("Domain marked as scam!");
		} else {
			event.respond("Please use only domain, excluding https and path!");
		}
	}

	private static void allowDomain(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		String domain = event.get("domain").asString();

		if (ScamHandler.EXACT_DOMAIN.matcher(domain).matches()) {
			ScamHandler.setOverride(event.context.gc, event.context.sender.getTag(), domain, ScamHandler.Type.ALLOW);
			event.respond("Domain marked as false positive!");
		} else {
			event.respond("Please use only domain, excluding https and path!");
		}
	}

	private static void removeDomain(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		String domain = event.get("domain").asString();

		if (ScamHandler.EXACT_DOMAIN.matcher(domain).matches()) {
			ScamHandler.setOverride(event.context.gc, event.context.sender.getTag(), domain, ScamHandler.Type.DEFAULT);
			event.respond("Domain override removed!");
		} else {
			event.respond("Please use only domain, excluding https and path!");
		}
	}

	private static void test(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		String text = event.get("text").asString();

		ScamHandler.Scam scam = ScamHandler.checkScam(text);

		if (scam == null) {
			event.respond("This message is safe!");
		} else {
			event.respond("Found a scam:\n" + scam);
		}
	}
}