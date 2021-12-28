package net.emustudio.plugins.compiler.as8080;

import java.util.Optional;

public class Either<L, R> {
    public final L left;
    public final R right;

    private Either(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public Optional<L> left() {
        return Optional.ofNullable(left);
    }

    public Optional<R> right() {
        return Optional.ofNullable(right);
    }

    public boolean isLeft() {
        return left != null;
    }

    public boolean isRight() {
        return right != null;
    }

    public static <L,R> Either<L, R> ofLeft(L left) {
        return new Either<>(left, (R) null);
    }

    public static <L, R> Either<L, R> ofRight(R right) {
        return new Either<>((L) null, right);
    }
}
