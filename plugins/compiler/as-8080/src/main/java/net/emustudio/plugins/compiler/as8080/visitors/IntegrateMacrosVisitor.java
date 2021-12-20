package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprId;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroArgument;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroCall;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroDef;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Replaces ExprIds inside macro definition with macro arguments, and removes PseudoMacroCall + PseudoMacroDef.
 * Macro expansions disappear and just code remains.
 */
public class IntegrateMacrosVisitor extends NodeVisitor {
    private final List<String> macroParameters = new ArrayList<>();
    private final List<Node> macroArguments = new ArrayList<>();

    private boolean insideMacroParameter;
    private boolean insideMacroDef;

    @Override
    public void visit(ExprId node) {
        String id = node.id.toLowerCase(Locale.ENGLISH);

        if (insideMacroParameter) {
            macroParameters.add(id);
        } else if (insideMacroDef) {
            int index = macroParameters.indexOf(id);
            Node argument = macroArguments.get(index);
            node.remove().ifPresent(p -> p.addChild(argument));
        }
    }

    @Override
    public void visit(PseudoMacroArgument node) {
        macroArguments.add(node.getChild(0));
        node.remove();
    }

    @Override
    public void visit(PseudoMacroParameter node) {
        insideMacroParameter = true;
        visitChildren(node);
        node.remove();
        insideMacroParameter = false;
    }

    @Override
    public void visit(PseudoMacroDef node) {
        insideMacroDef = true;
        visitChildren(node);
        node.remove().ifPresent(p -> p.addChildren(node.getChildren()));
        insideMacroDef = false;
    }

    @Override
    public void visit(PseudoMacroCall node) {
        visitChildren(node);
        node.remove().ifPresent(p -> p.addChildren(node.getChildren()));
    }
}
