package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Label;
import net.emustudio.plugins.compiler.as8080.ast.Node;
import net.emustudio.plugins.compiler.as8080.ast.NodeVisitor;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprId;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoEqu;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroDef;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroParameter;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoSet;

import java.util.*;

import static net.emustudio.plugins.compiler.as8080.CompileError.alreadyDeclared;

/**
 * Checks if all declarations are valid:
 * - ID of constant,variable and macro can be used just once in whole program.
 * - macro parameters should not conflict with:
 *   - declarations in and out of the macro scope
 *   - previously declared parameters in current or parent macros if the current one is nested
 */
public class CheckDeclarationsVisitor extends NodeVisitor {
    private final Set<String> allDeclarations = new HashSet<>();
    private final Set<String> macros = new HashSet<>();
    private final Set<String> variables = new HashSet<>();

    // for checking collisions with other declarations (e.g. forward referenced labels)
    private final Set<String> allMacroParams = new HashSet<>();

    // for checking marco param names in nested macros
    private final List<Set<String>> macroParamsInScope = new ArrayList<>();

    // for easier removal of current macro params from macroParamsInScope when the macro definition ends
    private Set<String> currentMacroParams;

    private boolean insideMacroParameter = false;

    @Override
    public void visit(PseudoEqu node) {
        addDeclaration(node.id, node);
    }

    @Override
    public void visit(PseudoMacroDef node) {
        addMacro(node.id, node);
        currentMacroParams = new HashSet<>();
        macroParamsInScope.add(currentMacroParams);
        visitChildren(node);
        macroParamsInScope.remove(macroParamsInScope.size() - 1);
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

    @Override
    public void visit(PseudoMacroParameter node) {
        insideMacroParameter = true;
        visitChildren(node);
        insideMacroParameter = false;
    }

    @Override
    public void visit(ExprId node) {
        if (insideMacroParameter) {
            addMacroParameter(node.id, node);
        }
    }

    private void addVariable(String id, Node node) {
        addDeclaration(id, true, node);
    }

    private void addDeclaration(String id, Node node) {
        addDeclaration(id, false, node);
    }

    private void addDeclaration(String id, boolean isVariable, Node node) {
        String idLower = id.toLowerCase(Locale.ENGLISH);

        if (allMacroParams.contains(idLower) || buildDeclarations().contains(idLower) && (!isVariable || !variables.contains(id))) {
            error(alreadyDeclared(node, id));
        }
        allDeclarations.add(idLower);
        if (isVariable) {
            variables.add(idLower);
        }
    }

    private void addMacro(String id, Node node) {
        String idLower = id.toLowerCase(Locale.ENGLISH);
        if (macros.contains(idLower)) {
            error(alreadyDeclared(node, id));
        }
        macros.add(idLower);
    }

    private void addMacroParameter(String id, Node node) {
        String idLower = id.toLowerCase(Locale.ENGLISH);
        if (buildDeclarations().contains(idLower) || macros.contains(idLower)) {
            error(alreadyDeclared(node, id));
        }
        currentMacroParams.add(idLower);
        allMacroParams.add(idLower);
    }

    private Set<String> buildDeclarations() {
        Set<String> allDeclarations = new HashSet<>();
        for (Set<String> parameters : macroParamsInScope) {
            allDeclarations.addAll(parameters);
        }
        allDeclarations.addAll(this.allDeclarations);
        return allDeclarations;
    }
}
