package dev.gnomebot.app.util;

/**
 * @author LatvianModder
 */
public abstract class Either<L, R> {
	public static <L, R> Either<L, R> left(L l) {
		return new Left<>(l);
	}

	public static <L, R> Either<L, R> right(R r) {
		return new Right<>(r);
	}

	public abstract L getLeft();

	public abstract R getRight();

	public abstract boolean isLeft();

	public abstract boolean isRight();

	private static class Left<L, R> extends Either<L, R> {
		private final L left;

		public Left(L l) {
			left = l;
		}

		@Override
		public L getLeft() {
			return left;
		}

		@Override
		public R getRight() {
			throw new NullPointerException("This is Left side!");
		}

		@Override
		public boolean isLeft() {
			return true;
		}

		@Override
		public boolean isRight() {
			return false;
		}

		@Override
		public String toString() {
			return "Left{" + left + '}';
		}
	}

	private static class Right<L, R> extends Either<L, R> {
		private final R right;

		public Right(R r) {
			right = r;
		}

		@Override
		public L getLeft() {
			throw new NullPointerException("This is Right side!");
		}

		@Override
		public R getRight() {
			return right;
		}

		@Override
		public boolean isLeft() {
			return false;
		}

		@Override
		public boolean isRight() {
			return true;
		}

		@Override
		public String toString() {
			return "Right{" + right + '}';
		}
	}
}
