package net.emustudio.plugins.compiler.as8080.ast.instr;

import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.emustudio.plugins.compiler.as8080.As8080Parser.*;

public class InstrExpr extends Node {
    public final int opcode;
    private final static Set<Integer> twoBytes = new HashSet<>();
    private final static Map<Integer, Integer> opcodes = new HashMap<>();

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

        opcodes.put(OPCODE_LDA, 0x3A);
        opcodes.put(OPCODE_STA, 0x32);
        opcodes.put(OPCODE_LHLD, 0x2A);
        opcodes.put(OPCODE_SHLD, 0x22);
        opcodes.put(OPCODE_ADI, 0xC6);
        opcodes.put(OPCODE_ACI, 0xCE);
        opcodes.put(OPCODE_SUI, 0xD6);
        opcodes.put(OPCODE_SBI, 0xDE);
        opcodes.put(OPCODE_ANI, 0xE6);
        opcodes.put(OPCODE_ORI, 0xF6);
        opcodes.put(OPCODE_XRI, 0xEE);
        opcodes.put(OPCODE_CPI, 0xFE);
        opcodes.put(OPCODE_JMP, 0xC3);
        opcodes.put(OPCODE_JC, 0xDA);
        opcodes.put(OPCODE_JNC, 0xD2);
        opcodes.put(OPCODE_JZ, 0xCA);
        opcodes.put(OPCODE_JNZ, 0xC2);
        opcodes.put(OPCODE_JM, 0xFA);
        opcodes.put(OPCODE_JP, 0xF2);
        opcodes.put(OPCODE_JPE, 0xEA);
        opcodes.put(OPCODE_JPO, 0xE2);
        opcodes.put(OPCODE_CALL, 0xCD);
        opcodes.put(OPCODE_CC, 0xDC);
        opcodes.put(OPCODE_CNC, 0xD4);
        opcodes.put(OPCODE_CNZ, 0xC4);
        opcodes.put(OPCODE_CM, 0xFC);
        opcodes.put(OPCODE_CP, 0xF4);
        opcodes.put(OPCODE_CPE, 0xEC);
        opcodes.put(OPCODE_CPO, 0xE4);
        opcodes.put(OPCODE_IN, 0xDB);
        opcodes.put(OPCODE_OUT, 0xD3);
        opcodes.put(OPCODE_RST, 0xC7);
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

    public byte eval() {
        byte result = (byte) (opcodes.get(opcode) & 0xFF);
        if (opcode == OPCODE_RST) {
            result = (byte) (result | collectChild(Evaluated.class)
                .map(e -> {
                    int value = e.value;
                    return (byte) (value << 3);
                }).orElse((byte) 0));
        }

        return result;
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
