package net.emustudio.plugins.compiler.asZ80.ast.instr;

import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.visitors.NodeVisitor;
import org.antlr.v4.runtime.Token;

import java.util.*;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;

public class InstrN extends Node {
    // opcode=OPCODE_JR n=rExpression
    // opcode=OPCODE_ADD REG_A SEP_COMMA n=rExpression
    // opcode=OPCODE_ADC REG_A SEP_COMMA n=rExpression
    // opcode=OPCODE_OUT SEP_LPAR n=rExpression SEP_RPAR SEP_COMMA REG_A
    // opcode=OPCODE_SUB n=rExpression
    // opcode=OPCODE_IN REG_A SEP_COMMA SEP_LPAR n=rExpression SEP_RPAR
    // opcode=OPCODE_SBC REG_A SEP_COMMA n=rExpression
    // opcode=OPCODE_AND n=rExpression
    // opcode=OPCODE_XOR n=rExpression
    // opcode=OPCODE_OR n=rExpression
    // opcode=OPCODE_CP n=rExpression
    // opcode=OPCODE_RST n=rExpression

    public final int opcode;
    private final static Set<Integer> twoBytes = new HashSet<>();
    private final static Map<Integer, Integer> opcodes = new HashMap<>();

    static {
        twoBytes.add(OPCODE_JR);
        twoBytes.add(OPCODE_ADD);
        twoBytes.add(OPCODE_ADC);
        twoBytes.add(OPCODE_SUB);
        twoBytes.add(OPCODE_SBC);
        twoBytes.add(OPCODE_AND);
        twoBytes.add(OPCODE_XOR);
        twoBytes.add(OPCODE_OR);
        twoBytes.add(OPCODE_CP);
        twoBytes.add(OPCODE_IN);
        twoBytes.add(OPCODE_OUT);

        opcodes.put(OPCODE_JR, 0x18);
        opcodes.put(OPCODE_JP, 0xC3);
        opcodes.put(OPCODE_CALL, 0xCD);
        opcodes.put(OPCODE_ADD, 0xC6);
        opcodes.put(OPCODE_ADC, 0xCE);
        opcodes.put(OPCODE_SUB, 0xD6);
        opcodes.put(OPCODE_SBC, 0xDE);
        opcodes.put(OPCODE_AND, 0xE6);
        opcodes.put(OPCODE_XOR, 0xEE);
        opcodes.put(OPCODE_OR, 0xF6);
        opcodes.put(OPCODE_CP, 0xFE);
        opcodes.put(OPCODE_RST, 0xC7);
        opcodes.put(OPCODE_IN, 0xDB);
        opcodes.put(OPCODE_OUT, 0xD3);



//        opcodes.put(OPCODE_STA, 0x32);
//        opcodes.put(OPCODE_LHLD, 0x2A);
//        opcodes.put(OPCODE_SHLD, 0x22);
//        opcodes.put(OPCODE_JC, 0xDA);
//        opcodes.put(OPCODE_JNC, 0xD2);
//        opcodes.put(OPCODE_JZ, 0xCA);
//        opcodes.put(OPCODE_JNZ, 0xC2);
//        opcodes.put(OPCODE_JM, 0xFA);
//        opcodes.put(OPCODE_JP, 0xF2);
//        opcodes.put(OPCODE_JPE, 0xEA);
//        opcodes.put(OPCODE_JPO, 0xE2);
//        opcodes.put(OPCODE_CC, 0xDC);
//        opcodes.put(OPCODE_CZ, 0xCC);
//        opcodes.put(OPCODE_CNC, 0xD4);
//        opcodes.put(OPCODE_CNZ, 0xC4);
//        opcodes.put(OPCODE_CM, 0xFC);
//        opcodes.put(OPCODE_CP, 0xF4);
//        opcodes.put(OPCODE_CPE, 0xEC);
//        opcodes.put(OPCODE_CPO, 0xE4);
    }

    public InstrN(int line, int column, int opcode) {
        super(line, column);
        this.opcode = opcode;
        // child is expr
    }

    public InstrN(Token opcode) {
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

    public Optional<Byte> eval() {
        byte result = (byte) (opcodes.get(opcode) & 0xFF);
        if (opcode == OPCODE_RST) {
            return collectChild(Evaluated.class)
                .filter(e -> e.value >= 0 && e.value <= 7)
                .map(e -> (byte) (result | (e.value << 3)));
        }

        return Optional.of(result);
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
        return new InstrN(line, column, opcode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InstrN instrExpr = (InstrN) o;
        return opcode == instrExpr.opcode;
    }
}
