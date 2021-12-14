package net.emustudio.plugins.compiler.as8080.ast;

import net.emustudio.plugins.compiler.as8080.CompileError;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import net.emustudio.plugins.compiler.as8080.ast.data.DataPlainString;
import net.emustudio.plugins.compiler.as8080.ast.expr.*;
import net.emustudio.plugins.compiler.as8080.ast.instr.*;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.*;
import net.emustudio.plugins.compiler.as8080.exceptions.used.FatalError;

public class NodeVisitor {
    protected NameSpace env;

    protected void error(CompileError error) {
        env.error(error);
    }

    protected void fatalError(CompileError error) {
        FatalError.now(error);
    }

    public void visit(Node node) {
        visitChildren(node);
    }

    public void visit(Program node) {
        if (env == null) {
            this.env = node.env();
        }
        visitChildren(node);
    }

    public void visit(DataDB node) {
        visitChildren(node);
    }

    public void visit(DataDW node) {
        visitChildren(node);
    }

    public void visit(DataDS node) {
        visitChildren(node);
    }

    public void visit(DataPlainString node) {
        visitChildren(node);
    }

    public void visit(ExprCurrentAddress node) {
        visitChildren(node);
    }

    public void visit(ExprInfix node) {
        visitChildren(node);
    }

    public void visit(ExprId node) {
        visitChildren(node);
    }

    public void visit(ExprNumber node) {
        visitChildren(node);
    }

    public void visit(ExprUnary node) {
        visitChildren(node);
    }

    public void visit(InstrExpr node) {
        visitChildren(node);
    }

    public void visit(InstrNoArgs node) {
        visitChildren(node);
    }

    public void visit(InstrReg node) {
        visitChildren(node);
    }

    public void visit(InstrRegExpr node) {
        visitChildren(node);
    }

    public void visit(InstrRegPair node) {
        visitChildren(node);
    }

    public void visit(InstrRegPairExpr node) {
        visitChildren(node);
    }

    public void visit(InstrRegReg node) {
        visitChildren(node);
    }

    public void visit(PseudoEqu node) {
        visitChildren(node);
    }

    public void visit(PseudoIf node) {
        visitChildren(node);
    }

    public void visit(PseudoInclude node) {
        visitChildren(node);
    }

    public void visit(PseudoMacroCall node) {
        visitChildren(node);
    }

    public void visit(PseudoMacroDef node) {
        visitChildren(node);
    }

    public void visit(PseudoOrg node) {
        visitChildren(node);
    }

    public void visit(PseudoSet node) {
        visitChildren(node);
    }

    public void visit(Label node) {
        visitChildren(node);
    }

    protected void visitChildren(Node node) {
        for (Node child : node.getChildren()) {
            child.accept(this);
        }
    }
}
