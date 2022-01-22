package dev.gnomebot.app.discord;

public enum Confirm {
	QUERY,
	YES,
	NO;

	public static Confirm of(String[] path, int index) {
		if (path.length > index) {
			if (path[index].equals("yes")) {
				return YES;
			} else if (path[index].equals("no")) {
				return NO;
			}
		}

		return QUERY;
	}
}
