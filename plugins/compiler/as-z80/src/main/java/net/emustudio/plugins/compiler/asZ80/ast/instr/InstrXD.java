package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

public class InstrXD extends Node {
    public final int opcode;
    public final int prefix;
    public final int x;
    public final int y;
    public final int z;

    public InstrXD(int line, int column, int opcode, int prefix, int x, int y, int z) {
        super(line, column);
        this.opcode = opcode;
        this.prefix = prefix;
        this.x = x;
        this.y = y;
        this.z = z;

        // 1. child is maybe expr (II+d) or N
        // 2. child is maybe N if (II+d) is defined
    }

    public InstrXD(Token opcode, int prefix, int x, int y, int z) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), prefix, x, y, z);
    }

    public InstrXD(Token opcode, int prefix, int x, int q, int p, int z) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), prefix, x, (p << 1) | q, z);
    }

    public byte[] eval() {
        return new byte[] {
            (byte)prefix,
            (byte)(((x << 6) | (y << 3) | (z & 7)) & 0xFF)
        };
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new InstrXD(line, column, opcode, prefix, x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InstrXD instr = (InstrXD) o;
        return opcode == instr.opcode && prefix == instr.prefix && x == instr.x && y == instr.y && z == instr.z;
    }

    @Override
    protected String toStringShallow() {
        return "InstrXD(" + opcode + ", prefix=" + prefix + ", x=" + x + ", y=" + y + ", z=" + z + ")";
    }

}
