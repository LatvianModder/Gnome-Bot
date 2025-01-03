package dev.gnomebot.app.data;

import dev.latvian.apps.ansi.log.Log;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class LazyOptional<T> {
	@FunctionalInterface
	public interface LazySupplier<T> {
		T get() throws Exception;
	}

	public static final LazyOptional<?> EMPTY = of(() -> null);

	public static <T> LazyOptional<T> of(LazySupplier<T> getter) {
		return new LazyOptional<>(getter);
	}

	@SuppressWarnings("unchecked")
	public static <T> LazyOptional<T> empty() {
		return (LazyOptional<T>) EMPTY;
	}

	private final LazySupplier<T> getter;
	private T object;
	private boolean inited;

	private LazyOptional(LazySupplier<T> g) {
		getter = g;
	}

	public void invalidate() {
		object = null;
		inited = false;
	}

	@Nullable
	public T get() {
		if (!inited) {
			inited = true;

			try {
				object = getter.get();
			} catch (Exception ex) {
				Log.error(ex);
			}
		}

		return object;
	}

	public void set(T v) {
		inited = true;
		object = v;
	}

	public boolean isPresent() {
		return get() != null;
	}

	public T getNonnull() {
		var t = get();

		if (t == null) {
			throw new NullPointerException();
		}

		return t;
	}

	public Optional<T> getOptional() {
		return Optional.ofNullable(get());
	}
}