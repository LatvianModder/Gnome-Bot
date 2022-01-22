package dev.gnomebot.app.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Function;

public class MappedIterable<F, T> implements Iterable<T> {
	private final Iterable<F> parent;
	private final Function<F, T> factory;

	public MappedIterable(Iterable<F> p, Function<F, T> f) {
		parent = p;
		factory = f;
	}

	@NotNull
	@Override
	public Iterator<T> iterator() {
		return new MappedIterator<>(parent.iterator(), factory);
	}
}
