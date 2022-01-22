package dev.gnomebot.app.cli;

@FunctionalInterface
public interface CLICallback {
	void run(CLIEvent event) throws Exception;
}
