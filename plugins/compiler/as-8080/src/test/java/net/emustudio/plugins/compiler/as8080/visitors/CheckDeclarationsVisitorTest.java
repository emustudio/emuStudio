package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.exceptions.AlreadyDeclaredException;
import org.junit.Test;

import static net.emustudio.plugins.compiler.as8080.Utils.parseProgram;

public class CheckDeclarationsVisitorTest {

    @Test
    public void testDeclarationsAreFound() {
        Program program = parseProgram("label:\nconstant equ 1\nvariable set 1\nxxx macro\nendm");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
    }

    @Test
    public void testVariableTwoTimesPass() {
        Program program = parseProgram("var set 1\nvar set 2");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
    }

    @Test(expected = AlreadyDeclaredException.class)
    public void testConstantTwoTimesCannotBeDefined() {
        Program program = parseProgram("var equ 1\nvar equ 2");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
    }

    @Test(expected = AlreadyDeclaredException.class)
    public void testVarCannotBeDefinedIfAnotherDeclarationExists() {
        Program program = parseProgram("var equ 1\nvar set 2");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
    }

    @Test
    public void testMacrosAndDeclarationsAreIndependent() {
        Program program = parseProgram("label:\nlabel macro\nendm");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
    }

    @Test(expected = AlreadyDeclaredException.class)
    public void testLabelThenConstantAlreadyDeclared() {
        Program program = parseProgram("label:\nlabel equ 1");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
    }

    @Test(expected = AlreadyDeclaredException.class)
    public void testMacroAlreadyDeclared() {
        Program program = parseProgram("label macro\nendm\nlabel macro\nendm");
        CheckDeclarationsVisitor visitor = new CheckDeclarationsVisitor();
        visitor.visit(program);
    }
}
