package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.Optional;
import java.util.Set;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.OPCODE_RST;

public class Instr extends Node {
    public final int opcode;
    public final int x;
    public final int y;
    public final int z;
    private final static Set<Integer> allowedRstValues = Set.of(
        0, 0x8, 0x10, 0x18, 0x20, 0x28, 0x30, 0x38
    );

    public Instr(int line, int column, int opcode, int x, int y, int z) {
        super(line, column);
        this.opcode = opcode;
        this.x = x;
        this.y = y;
        this.z = z;

        // children might be expr in the same order as when compiled
    }

    public Instr(Token opcode, int x, int y, int z) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), x, y, z);
    }

    public Instr(Token opcode, int x, int q, int p, int z) {
        this(opcode.getLine(), opcode.getCharPositionInLine(), opcode.getType(), x, (p << 1) | q, z);
    }

    public Optional<Byte> eval() {
        Optional<Integer> actualY = Optional.of(y);
        if (opcode == OPCODE_RST) {
            actualY = collectChild(Evaluated.class)
                .map(e -> e.value)
                .filter(allowedRstValues::contains)
                .map(v -> v / 8);
        }
        return actualY.map(yy -> (byte)(((x << 6) | (yy << 3) | (z & 7)) & 0xFF));
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Node mkCopy() {
        return new Instr(line, column, opcode, x, y, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Instr instr = (Instr) o;
        return opcode == instr.opcode && x == instr.x && y == instr.y && z == instr.z;
    }

    @Override
    protected String toStringShallow() {
        return "Instr(" + opcode + ",  x=" + x + ", y=" + y + ", z=" + z + ")";
    }
}
