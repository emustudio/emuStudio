package net.emustudio.plugins.compiler.as8080.ast.data;

import net.emustudio.plugins.compiler.as8080.Either;
import net.emustudio.plugins.compiler.as8080.ast.*;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

import static net.emustudio.plugins.compiler.as8080.ParsingUtils.parseLitString;

public class DataPlainString extends Node {
    public final String string;

    public DataPlainString(int line, int column, String string) {
        super(line, column);
        this.string = Objects.requireNonNull(string);
    }

    public DataPlainString(Token string) {
        this(string.getLine(), string.getCharPositionInLine(), parseLitString(string));
    }

    @Override
    public Either<Node, Evaluated> eval(int currentAddress, int expectedSizeBytes, NameSpace env) {
        int sizeBytes = string.length();
        Evaluated evaluated = new Evaluated(line, column, currentAddress, sizeBytes);
        for (byte b : string.getBytes()) {
            evaluated.addChild(new ExprNumber(line, column, b));
        }
        return Either.ofRight(evaluated);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "DataPlainString('" + string + "')";
    }

    @Override
    protected Node mkCopy() {
        return new DataPlainString(line, column, string);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataPlainString that = (DataPlainString) o;
        return Objects.equals(string, that.string);
    }
}
