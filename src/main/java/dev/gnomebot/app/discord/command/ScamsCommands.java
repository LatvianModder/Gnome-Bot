package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.ScamHandler;

public class ScamsCommands extends ApplicationCommands {
	public static void fetchDomains(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderTrusted();
		ScamHandler.fetchDomains(() -> event.respond("Done!"));
	}

	public static void checkDomain(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		var domain = event.get("domain").asString();

		if (ScamHandler.EXACT_DOMAIN.matcher(domain).matches()) {
			event.respond("Domain type: " + ScamHandler.checkScamDomain(domain).commandName);
		} else {
			event.respond("Please use only domain, excluding https and path!");
		}
	}

	public static void blockDomain(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		var domain = event.get("domain").asString();

		if (ScamHandler.EXACT_DOMAIN.matcher(domain).matches()) {
			ScamHandler.setOverride(event.context.gc, event.context.sender.getTag(), domain, ScamHandler.Type.BLOCK);
			event.respond("Domain marked as scam!");
		} else {
			event.respond("Please use only domain, excluding https and path!");
		}
	}

	public static void allowDomain(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		var domain = event.get("domain").asString();

		if (ScamHandler.EXACT_DOMAIN.matcher(domain).matches()) {
			ScamHandler.setOverride(event.context.gc, event.context.sender.getTag(), domain, ScamHandler.Type.ALLOW);
			event.respond("Domain marked as false positive!");
		} else {
			event.respond("Please use only domain, excluding https and path!");
		}
	}

	public static void removeDomain(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		var domain = event.get("domain").asString();

		if (ScamHandler.EXACT_DOMAIN.matcher(domain).matches()) {
			ScamHandler.setOverride(event.context.gc, event.context.sender.getTag(), domain, ScamHandler.Type.DEFAULT);
			event.respond("Domain override removed!");
		} else {
			event.respond("Please use only domain, excluding https and path!");
		}
	}

	public static void test(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		var text = event.get("text").asString();

		var scam = ScamHandler.checkScam(text);

		if (scam == null) {
			event.respond("This message is safe!");
		} else {
			event.respond("Found a scam:\n" + scam);
		}
	}
}