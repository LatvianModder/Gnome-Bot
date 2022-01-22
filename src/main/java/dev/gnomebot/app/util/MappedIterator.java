package dev.gnomebot.app.util;

import java.util.Iterator;
import java.util.function.Function;

public class MappedIterator<F, T> implements Iterator<T> {
	private final Iterator<F> parent;
	private final Function<F, T> factory;

	public MappedIterator(Iterator<F> p, Function<F, T> f) {
		parent = p;
		factory = f;
	}

	@Override
	public boolean hasNext() {
		return parent.hasNext();
	}

	@Override
	public T next() {
		return factory.apply(parent.next());
	}
}
