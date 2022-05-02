package net.emustudio.plugins.compiler.asZ80;

public class Pair<L,R> {
    public final L l;
    public final R r;

    public Pair(L l, R r) {
        this.l = l;
        this.r = r;
    }

    public static <L, R> Pair<L, R> of(L l, R r) {
        return new Pair<>(l, r);
    }
}
