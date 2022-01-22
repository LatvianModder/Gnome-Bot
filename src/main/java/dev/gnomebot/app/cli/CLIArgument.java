package dev.gnomebot.app.cli;

public class CLIArgument {
	public enum Type {
		STRING,
		INT,
		FLOAT,
		CHANNEL,
		USER,
		MESSAGE,
	}

	public final char name;
	public final Type type;

	public CLIArgument(char n, Type t) {
		name = n;
		type = t;
	}
}
