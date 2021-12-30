package net.emustudio.plugins.compiler.as8080.ast.instr;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.HashSet;
import java.util.Set;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;

public class InstrExpr extends Node {
    public final int opcode;
    private final static Set<Integer> twoBytes = new HashSet<>();

    static {
        twoBytes.add(OPCODE_ADI);
        twoBytes.add(OPCODE_ACI);
        twoBytes.add(OPCODE_SUI);
        twoBytes.add(OPCODE_SBI);
        twoBytes.add(OPCODE_ANI);
        twoBytes.add(OPCODE_ORI);
        twoBytes.add(OPCODE_XRI);
        twoBytes.add(OPCODE_CPI);
        twoBytes.add(OPCODE_IN);
        twoBytes.add(OPCODE_OUT);
    }

    public InstrExpr(int line, int column, int opcode) {
        super(line, column);
        this.opcode = opcode;
        // child is expr
    }

    public InstrExpr(Token opcode) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType());
    }

    public int getExprSizeBytes() {
        if (opcode == OPCODE_RST) {
            return 0;
        } else if (twoBytes.contains(opcode)) {
            return 1;
        }
        return 2; // address
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrExpr(" + opcode + ")";
    }

    @Override
    protected Node mkCopy() {
        return new InstrExpr(line, column, opcode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrExpr instrExpr = (InstrExpr) o;
        return opcode == instrExpr.opcode;
    }
}
