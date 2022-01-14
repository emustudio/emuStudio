package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.plugins.compiler.as8080.ast.Evaluated;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import net.emustudio.plugins.compiler.as8080.ast.instr.*;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoOrg;

import java.util.Objects;

public class GenerateCodeVisitor extends NodeVisitor {
    private final IntelHEX hex;

    private int currentAddress;
    private Evaluated lastEval;

    public GenerateCodeVisitor(IntelHEX hex) {
        this.hex = Objects.requireNonNull(hex);
    }

    @Override
    public void visit(DataDB node) {
        super.visit(node);
    }

    @Override
    public void visit(DataDW node) {
        super.visit(node);
    }

    @Override
    public void visit(DataDS node) {
        super.visit(node);
    }

    @Override
    public void visit(InstrExpr node) {
        super.visit(node);
    }

    @Override
    public void visit(InstrNoArgs node) {
        super.visit(node);
    }

    @Override
    public void visit(InstrReg node) {
        super.visit(node);
    }

    @Override
    public void visit(InstrRegExpr node) {
        super.visit(node);
    }

    @Override
    public void visit(InstrRegPair node) {
        super.visit(node);
    }

    @Override
    public void visit(InstrRegPairExpr node) {
        super.visit(node);
    }

    @Override
    public void visit(InstrRegReg node) {
        super.visit(node);
    }

    @Override
    public void visit(PseudoOrg node) {
        super.visit(node);
    }
}
