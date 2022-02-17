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
import static net.emustudio.plugins.compiler.asZ80.CompileError.valueOutOfBounds;

public class GenerateCodeVisitor extends NodeVisitor {
    private final IntelHEX hex;
    private int expectedBytes;

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
        node.eval().ifPresentOrElse(
            hex::add,
            () -> error(valueOutOfBounds(node, 0, 7))
        );
        int instrSize = node.getSizeBytes().orElse(1);
        if (instrSize > 1) {
            expectedBytes = 0;
            visitChildren(node);
        }
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
        for (byte b : node.eval()) {
            hex.add(b);
        }
        expectedBytes = 0;
        visitChildren(node);
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
        if (expectedBytes == 1) {
            addByte(node.value);
        } else if (expectedBytes == 2) {
            addWord(node.value);
        } else {
            node.getSizeBytes().ifPresent(size -> {
                if (size == 1) {
                    addByte(node.value);
                } else if (size == 2) {
                    addWord(node.value);
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
