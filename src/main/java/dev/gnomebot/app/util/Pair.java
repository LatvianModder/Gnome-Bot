package dev.gnomebot.app.util;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author LatvianModder
 */
public final class Pair<A, B> {
	public static <A, B> Pair<A, B> of(A a, B b) {
		return new Pair<>(a, b);
	}

	public final A a;
	public final B b;

	private Pair(A _a, B _b) {
		a = _a;
		b = _b;
	}

	@Override
	public String toString() {
		return "[" + a + "," + b + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o == null || o.getClass() != Pair.class) {
			return false;
		}

		Pair<?, ?> pair = (Pair<?, ?>) o;
		return Objects.equals(a, pair.a) && Objects.equals(b, pair.b);
	}

	public int hashCode() {
		return Objects.hashCode(a) * 31 + Objects.hashCode(b);
	}

	public <AN, BN> Pair<AN, BN> map(Function<A, AN> an, Function<B, BN> bn) {
		return of(an.apply(a), bn.apply(b));
	}

	public <BN> Pair<A, BN> map(Function<B, BN> bn) {
		return of(a, bn.apply(b));
	}

	public A getA() {
		return a;
	}

	public B getB() {
		return b;
	}
}