package net.emustudio.plugins.compiler.asZ80.visitors;

import net.emustudio.plugins.compiler.asZ80.CompileError;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.NameSpace;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.Program;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDS;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDW;
import net.emustudio.plugins.compiler.asZ80.ast.expr.*;
import net.emustudio.plugins.compiler.asZ80.ast.instr.*;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.*;
import net.emustudio.plugins.compiler.asZ80.exceptions.FatalError;

import java.util.List;

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

    public void visit(ExprCurrentAddress node) {
        visitChildren(node);
    }

    public void visit(ExprInfix node) {
        visitChildren(node);
    }

    public void visit(ExprId node) {
        visitChildren(node);
    }

    public void visit(ExprString node) {
        visitChildren(node);
    }

    public void visit(ExprNumber node) {
        visitChildren(node);
    }

    public void visit(ExprUnary node) {
        visitChildren(node);
    }

    public void visit(Instr node) {
        visitChildren(node);
    }

    public void visit(InstrCB node) {
        visitChildren(node);
    }

    public void visit(InstrED node) {
        visitChildren(node);
    }

    public void visit(InstrXD node) {
        visitChildren(node);
    }

    public void visit(InstrXDCB node) {
        visitChildren(node);
    }

    public void visit(PseudoEqu node) {
        visitChildren(node);
    }

    public void visit(PseudoIf node) {
        visitChildren(node);
    }

    public void visit(PseudoIfExpression node) {
        visitChildren(node);
    }

    public void visit(PseudoInclude node) {
        visitChildren(node);
    }

    public void visit(PseudoMacroCall node) {
        visitChildren(node);
    }

    public void visit(PseudoMacroArgument node) {
        visitChildren(node);
    }

    public void visit(PseudoMacroParameter node) {
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

    public void visit(PseudoLabel node) {
        visitChildren(node);
    }

    public void visit(Evaluated node) {
        visitChildren(node);
    }

    protected void visitChildren(Node node) {
        for (Node child : node.getChildren()) {
            child.accept(this);
        }
    }

    protected void visitChildren(Node node, int skipFirstN) {
        List<Node> children = node.getChildren();
        for (int i = skipFirstN; i < children.size(); i++) {
            children.get(i).accept(this);
        }
    }
}
