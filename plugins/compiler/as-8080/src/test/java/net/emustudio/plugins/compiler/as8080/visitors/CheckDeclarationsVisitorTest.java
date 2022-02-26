package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Program;
import org.junit.Test;

import static net.emustudio.plugins.compiler.as8080.CompileError.*;
import static net.emustudio.plugins.compiler.as8080.Utils.parseProgram;
import static org.junit.Assert.assertTrue;

public class CheckDeclarationsVisitorTest {

    @Test
    public void testDeclarationsAreFound() {
        Program program = parseProgram("label:\nconstant equ 1\nvariable set 1\nxxx macro\nendm");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
    }

    @Test
    public void testVariableTwoTimesPass() {
        Program program = parseProgram("variable set 1\nvariable set 2");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
    }

    @Test
    public void testConstantTwoTimesCannotBeDefined() {
        Program program = parseProgram("variable equ 1\nvariable equ 2");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_ALREADY_DECLARED));
    }

    @Test
    public void testVarCannotBeDefinedIfAnotherDeclarationExists() {
        Program program = parseProgram("variable equ 1\nvariable set 2");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_ALREADY_DECLARED));
    }

    @Test
    public void testMacrosAndDeclarationsAreIndependent() {
        Program program = parseProgram("label:\nlabel macro\nendm");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
    }

    @Test
    public void testLabelThenConstantAlreadyDeclared() {
        Program program = parseProgram("label:\nlabel equ 1");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_ALREADY_DECLARED));
    }

    @Test
    public void testMacroAlreadyDeclared() {
        Program program = parseProgram("label macro\nendm\nlabel macro\nendm");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_ALREADY_DECLARED));
    }

    @Test
    public void testMacroParametersCollideWithLabel() {
        Program program = parseProgram("label: x macro label\nendm");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_ALREADY_DECLARED));
    }

    @Test
    public void testMacroParametersCollideWithConstant() {
        Program program = parseProgram("const equ 1\nx macro const\nendm");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_ALREADY_DECLARED));
    }

    @Test
    public void testMacroParametersCollideWithVariable() {
        Program program = parseProgram("variable set 1\nx macro variable\nendm");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_ALREADY_DECLARED));
    }

    @Test
    public void testMacroParametersCollideParentMacroParameters() {
        Program program = parseProgram("x macro f,g,n\ny macro p,g,o\nendm\nendm");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_ALREADY_DECLARED));
    }

    @Test
    public void testMacroParametersAfterMacroEndStillCollideWithLabel() {
        Program program = parseProgram("x macro tt\nendm\ntt:");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_ALREADY_DECLARED));
    }

    @Test
    public void testMacroParametersCollideWithConstantInsideMacro() {
        Program program = parseProgram("x macro tt\ntt equ 1\nendm");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_ALREADY_DECLARED));
    }

    @Test
    public void testMacroNameCollidesWithParameterName() {
        Program program = parseProgram("x macro x\nendm");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_ALREADY_DECLARED));
    }

    @Test
    public void testMacroReuseParameterNamesIsPossible() {
        Program program = parseProgram("x macro t\nendm\ny macro t\nendm");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testIfExpressionReferencesOwnBlockDeclarations() {
        Program program = parseProgram("if variable + 1\nvariable set -1\nendif");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_IF_EXPRESSION_REFERENCES_OWN_BLOCK));
    }

    @Test
    public void testNestedIfExpressionReferencesOwnBlockDeclarations() {
        Program program = parseProgram("if variable + 1\nif 0\nvariable:\nendif\nendif");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_IF_EXPRESSION_REFERENCES_OWN_BLOCK));
    }

    @Test
    public void testSelfReferencingConstant() {
        Program program = parseProgram("self equ self + 1");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_DECLARATION_REFERENCES_ITSELF));
    }

    @Test
    public void testSelfReferencingNoneExistingVariableIsNotFine() {
        Program program = parseProgram("self set self + 1");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasError(ERROR_DECLARATION_REFERENCES_ITSELF));
    }

    @Test
    public void testSelfReferencingExistingVariableIsFine() {
        Program program = parseProgram("self set 1\n self set self + 1");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasNoErrors());
    }

    @Test
    public void testConstantIfReference() {
        Program program = parseProgram("const equ 1\nif const\nrrc\nendif");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
        assertTrue(program.env().hasNoErrors());
    }
}
