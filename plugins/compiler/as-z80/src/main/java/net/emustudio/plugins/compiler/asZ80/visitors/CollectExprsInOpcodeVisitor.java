package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.instr.Instr;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrCB;
import net.emustudio.plugins.compiler.asZ80.ast.instr.InstrXDCB;

import java.util.Set;

import static net.emustudio.plugins.compiler.asZ80.AsZ80Parser.*;

public class CollectExprsInOpcodeVisitor extends NodeVisitor {
    private final static Set<Integer> allowedRstValues = Set.of(
        0, 0x8, 0x10, 0x18, 0x20, 0x28, 0x30, 0x38
    );

    @Override
    public void visit(Instr node) {
        if (node.opcode == OPCODE_RST) {
            node
                .collectChild(Evaluated.class)
                .map(e -> {
                    e.remove();
                    return e.value;
                })
                .filter(allowedRstValues::contains)
                .map(v -> v / 8)
                .ifPresent(node::setY);
        }
    }

    @Override
    public void visit(InstrCB node) {
        // SET, RES, BIT
        switch (node.opcode) {
            case OPCODE_SET:
            case OPCODE_RES:
            case OPCODE_BIT:
                node
                    .collectChild(Evaluated.class)
                    .map(e -> {
                        e.remove();
                        return e.value;
                    })
                    .ifPresent(node::setY);
        }
    }

    @Override
    public void visit(InstrXDCB node) {
        // SET, RES, BIT
        switch (node.opcode) {
            case OPCODE_SET:
            case OPCODE_RES:
            case OPCODE_BIT:
                node
                    .collectChild(Evaluated.class)
                    .map(e -> {
                        e.remove();
                        return e.value;
                    })
                    .ifPresent(node::setY);
        }
    }
}
