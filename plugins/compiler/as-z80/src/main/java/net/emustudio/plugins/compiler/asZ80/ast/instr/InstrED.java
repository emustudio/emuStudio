package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Map;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;

public class InstrED extends Node {
    public final int opcode;
    public final int x;
    public final int y;
    public final int z;

    private static final Map<Integer, Integer> xmap = new HashMap<>();
    static {
        xmap.put(OPCODE_IN, 1);
        xmap.put(OPCODE_OUT, 1);
        xmap.put(OPCODE_SBC, 1);
        xmap.put(OPCODE_ADC, 1);
        xmap.put(OPCODE_LD, 1);
        xmap.put(OPCODE_NEG, 1);
        xmap.put(OPCODE_RETN, 1);
        xmap.put(OPCODE_RETI, 1);
        xmap.put(OPCODE_IM, 1);
        xmap.put(OPCODE_RRD, 1);
        xmap.put(OPCODE_RLD, 1);
        xmap.put(OPCODE_LDI, 2);
        xmap.put(OPCODE_LDD, 2);
        xmap.put(OPCODE_LDIR, 2);
        xmap.put(OPCODE_LDDR, 2);
        xmap.put(OPCODE_CPI, 2);
        xmap.put(OPCODE_CPD, 2);
        xmap.put(OPCODE_CPIR, 2);
        xmap.put(OPCODE_CPDR, 2);
        xmap.put(OPCODE_INI, 2);
        xmap.put(OPCODE_IND, 2);
        xmap.put(OPCODE_INIR, 2);
        xmap.put(OPCODE_INDR, 2);
        xmap.put(OPCODE_OUTI, 2);
        xmap.put(OPCODE_OUTD, 2);
        xmap.put(OPCODE_OTIR, 2);
        xmap.put(OPCODE_OTDR, 2);
    }

    public InstrED(int line, int column, int opcode, int y, int z) {
        super(line, column);
        this.opcode = opcode;
        this.x = xmap.get(opcode);
        this.y = y;
        this.z = z;

        // possibly child is expr
    }

    public InstrED(Token opcode, int y, int z) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), y, z);
    }

    public InstrED(Token opcode, int p, int q, int z) {
        this(opcode, (p << 1) | q, z);
    }

    public byte[] eval() {
        return new byte[] {
            (byte)0xED,
            (byte)(((x << 6) | (y << 3) | (z & 7)) & 0xFF)
        };
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new InstrED(line, column, opcode, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InstrED instr = (InstrED) o;
        return opcode == instr.opcode && x == instr.x && y == instr.y && z == instr.z;
    }

    @Override
    protected String toStringShallow() {
        return "InstrED(" + opcode + ",  x=" + x + ", y=" + y + ", z=" + z + ")";
    }

}
