package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Label;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoEqu;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroDef;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoSet;
import net.emustudio.plugins.compiler.as8080.exceptions.AlreadyDeclaredException;

import java.util.*;

public class CheckDeclarationsVisitor extends NodeVisitor {
    private final Set<String> declarations = new HashSet<>();
    private final Set<String> macros = new HashSet<>();
    private final Set<String> variables = new HashSet<>();

    @Override
    public void visit(PseudoEqu node) {
        addDeclaration(node.id, node);
    }

    @Override
    public void visit(PseudoMacroDef node) {
        addMacro(node.id, node);
        visitChildren(node);
    }

    @Override
    public void visit(PseudoSet node) {
        addVariable(node.id, node);
    }

    @Override
    public void visit(Label node) {
        addDeclaration(node.label, node);
        visitChildren(node);
    }

    private void addVariable(String id, Node node) {
        addDeclaration(id, true, node);
    }

    private void addDeclaration(String id, Node node) {
        addDeclaration(id, false, node);
    }

    private void addDeclaration(String id, boolean isVariable, Node node) {
        String idLower = id.toLowerCase(Locale.ENGLISH);

        if (declarations.contains(idLower) && (!isVariable || !variables.contains(id))) {
            throw new AlreadyDeclaredException(node.line, node.column, id);
        }
        declarations.add(idLower);
        if (isVariable) {
            variables.add(idLower);
        }
    }

    private void addMacro(String id, Node node) {
        String idLower = id.toLowerCase(Locale.ENGLISH);

        if (macros.contains(idLower)) {
            throw new AlreadyDeclaredException(node.line, node.column, id);
        }
        macros.add(idLower);
    }
}
