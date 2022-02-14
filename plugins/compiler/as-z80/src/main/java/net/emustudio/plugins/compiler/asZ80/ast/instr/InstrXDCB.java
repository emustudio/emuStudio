package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Map;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;

/**
 * DD or FD prefix, CB, displacement byte, opcode
 */
public class InstrXDCB extends Node {
    public final int opcode;
    public final int x;
    public final int y;
    public final int z;
    public final int prefix;

    private static final Map<Integer, Integer> xmap = new HashMap<>();
    static {
        xmap.put(OPCODE_RLC, 0);
        xmap.put(OPCODE_RRC, 0);
        xmap.put(OPCODE_RL, 0);
        xmap.put(OPCODE_RR, 0);
        xmap.put(OPCODE_SLA, 0);
        xmap.put(OPCODE_SRA, 0);
        xmap.put(OPCODE_SLL, 0);
        xmap.put(OPCODE_SRL, 0);
        xmap.put(OPCODE_BIT, 1);
        xmap.put(OPCODE_RES, 2);
        xmap.put(OPCODE_SET, 3);
    }

    public InstrXDCB(int line, int column, int opcode, int prefix, int y, int z) {
        super(line, column);

        this.opcode = opcode;
        this.prefix = prefix;
        this.x = xmap.get(opcode);
        this.y = y;
        this.z = z;

        // 1. child is expr (II + d)
        // 2. possibly another expr child
    }

    public InstrXDCB(Token opcode, int prefix, int y, int z) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), prefix, y, z);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new InstrXDCB(line, column, opcode, prefix, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InstrXDCB instr = (InstrXDCB) o;
        return opcode == instr.opcode && prefix == instr.prefix && x == instr.x && y == instr.y && z == instr.z;
    }

    @Override
    protected String toStringShallow() {
        return "InstrXDCB(" + opcode + ", prefix=" + prefix + ", x=" + x + ", y=" + y + ", z=" + z + ")";
    }
}
