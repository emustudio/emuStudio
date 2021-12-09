package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Label;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoEqu;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroDef;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoSet;

public class FindDeclarationsVisitor extends NodeVisitor {

    @Override
    public void visit(PseudoEqu node) {
        env.addDeclaration(node.id, node);
    }

    @Override
    public void visit(PseudoMacroDef node) {
        env.addMacro(node.id, node);
        visitChildren(node);
    }

    @Override
    public void visit(PseudoSet node) {
        env.addDeclaration(node.id, node);
    }

    @Override
    public void visit(Label node) {
        env.addDeclaration(node.label, node);
        visitChildren(node);
    }
}
