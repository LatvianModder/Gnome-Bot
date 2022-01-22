package dev.gnomebot.app.util;

import org.jetbrains.annotations.NotNull;

/**
 * @author LatvianModder
 */
public class Substitute implements Comparable<Substitute> {
	public final String with;
	public final String string;
	public final int order;

	public Substitute(String w, String s, int p) {
		with = w;
		string = s;
		order = p;
	}

	@Override
	public int compareTo(@NotNull Substitute o) {
		int i = Integer.compare(order, o.order);
		return i == 0 ? with.compareTo(o.with) : i;
	}

	@Override
	public String toString() {
		return string + "->" + with;
	}
}