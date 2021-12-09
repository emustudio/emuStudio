package net.emustudio.plugins.compiler.as8080.ast.instr;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class InstrReg extends Node {
    public final int opcode;
    public final int reg;

    public InstrReg(int line, int column, int opcode, int reg) {
        super(line, column);
        this.opcode = opcode;
        this.reg = reg;
    }

    public InstrReg(Token opcode, Token reg) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), reg.getType());
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected String toStringShallow() {
        return "InstrReg(" + opcode + "," + reg +")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrReg instrReg = (InstrReg) o;

        if (opcode != instrReg.opcode) return false;
        return reg == instrReg.reg;
    }

    @Override
    public int hashCode() {
        int result = opcode;
        result = 31 * result + reg;
        return result;
    }
}
