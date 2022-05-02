package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDS;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDW;
import net.emustudio.plugins.compiler.asZ80.ast.instr.*;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoOrg;

import java.util.Objects;

import static net.emustudio.plugins.compiler.asZ80.CompileError.valueMustBePositive;

public class GenerateCodeVisitor extends NodeVisitor {
    private final IntelHEX hex;
    private int expectedBytes;
    private boolean isRelative; // if we should treat Evaluated value as "relative address"
    private int currentAddress; // for computing relative address

    public GenerateCodeVisitor(IntelHEX hex) {
        this.hex = Objects.requireNonNull(hex);
    }

    @Override
    public void visit(DataDB node) {
        expectedBytes = 1;
        visitChildren(node);
    }

    @Override
    public void visit(DataDW node) {
        expectedBytes = 2;
        visitChildren(node);
    }

    @Override
    public void visit(DataDS node) {
        node.collectChild(Evaluated.class)
            .ifPresent(e -> {
                if (e.value < 0) {
                    error(valueMustBePositive(e));
                } else {
                    for (int i = 0; i < e.value; i++) {
                        hex.add((byte) 0);
                    }
                }
            });
    }

    @Override
    public void visit(Instr node) {
        isRelative = node.hasRelativeAddress();
        currentAddress = node.getAddress();

        hex.add(node.eval());
        int instrSize = node.getSizeBytes().orElse(1);
        if (instrSize > 1) {
            expectedBytes = 0;
            visitChildren(node);
        }
        isRelative = false;
    }

    @Override
    public void visit(InstrCB node) {
        for (byte b : node.eval()) {
            hex.add(b);
        }
        expectedBytes = 0;
        visitChildren(node);
    }

    @Override
    public void visit(InstrED node) {
        for (byte b : node.eval()) {
            hex.add(b);
        }
        expectedBytes = 0;
        visitChildren(node);
    }

    @Override
    public void visit(InstrXD node) {
        for (byte b : node.eval()) {
            hex.add(b);
        }
        expectedBytes = 0;
        visitChildren(node);
    }

    @Override
    public void visit(InstrXDCB node) {
        byte[] eval = node.eval();
        for (int i = 0; i < eval.length - 1; i++) {
            // opcode goes after displacement
            hex.add(eval[i]);
        }
        expectedBytes = 0;
        visitChildren(node);
        hex.add(eval[eval.length - 1]); // opcode
    }

    @Override
    public void visit(PseudoOrg node) {
        node.collectChild(Evaluated.class)
            .ifPresent(e -> {
                if (e.value < 0) {
                    error(valueMustBePositive(node));
                } else {
                    hex.setNextAddress(e.value);
                }
            });
    }

    @Override
    public void visit(Evaluated node) {
        final int value = (isRelative && node.isAddress) ? (node.value - currentAddress) : node.value;

        if (expectedBytes == 1) {
            addByte(value);
        } else if (expectedBytes == 2) {
            addWord(value);
        } else {
            node.getSizeBytes().ifPresent(size -> {
                if (size == 1) {
                    addByte(value);
                } else if (size == 2) {
                    addWord(value);
                }
            });
        }
    }

    private void addByte(int value) {
        hex.add((byte) (value & 0xFF));
    }

    private void addWord(int value) {
        byte byte0 = (byte) (value & 0xFF);
        byte byte1 = (byte) (value >>> 8);
        hex.add(byte0);
        hex.add(byte1);
    }
}
