package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Label;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDB;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDS;
import net.emustudio.plugins.compiler.as8080.ast.data.DataDW;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoEqu;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroDef;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoSet;

public class FindDeclarationsVisitor extends NodeVisitor {
    private NameSpace env;

    @Override
    public void visit(Program node) {
        this.env = node.env();
        visitChildren(node);
    }

    @Override
    public void visit(PseudoEqu node) {
        super.visit(node);
    }

    @Override
    public void visit(PseudoMacroDef node) {
        super.visit(node);
    }

    @Override
    public void visit(PseudoSet node) {
        super.visit(node);
    }

    @Override
    public void visit(Label node) {
        super.visit(node);
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
}
