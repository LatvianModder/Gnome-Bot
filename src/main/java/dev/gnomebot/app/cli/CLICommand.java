package dev.gnomebot.app.cli;

import java.util.ArrayList;
import java.util.List;

public class CLICommand {
	public static CLICommand make(String name) {
		CLICommand c = new CLICommand();
		c.name = name;
		return c;
	}

	public String name;
	public String description = "";
	public final List<CLIArgument> arguments = new ArrayList<>();
	public boolean admin = true;
	public boolean trusted = false;
	public CLICallback callback;

	public CLICommand description(String s) {
		description = s;
		return this;
	}

	public CLICommand noAdmin() {
		admin = false;
		return this;
	}

	public CLICommand trustedOnly() {
		trusted = true;
		return this;
	}

	public CLICommand arg(char name, CLIArgument.Type type) {
		arguments.add(new CLIArgument(name, type));
		return this;
	}

	public CLICommand arg(CLIArgument.Type type) {
		return arg((char) 0, type);
	}

	public CLICommand run(CLICallback event) {
		callback = event;
		return this;
	}
}
