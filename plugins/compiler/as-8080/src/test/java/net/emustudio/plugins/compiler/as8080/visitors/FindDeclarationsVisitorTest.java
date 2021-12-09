package net.emustudio.plugins.compiler.as8080.visitors;

import net.emustudio.plugins.compiler.as8080.ast.Label;
import net.emustudio.plugins.compiler.as8080.ast.NameSpace;
import net.emustudio.plugins.compiler.as8080.ast.Program;
import net.emustudio.plugins.compiler.as8080.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoEqu;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoMacroDef;
import net.emustudio.plugins.compiler.as8080.ast.pseudo.PseudoSet;
import org.junit.Test;

import java.util.Optional;

import static net.emustudio.plugins.compiler.as8080.Utils.parseProgram;
import static org.junit.Assert.assertEquals;

public class FindDeclarationsVisitorTest {

    @Test
    public void testDeclarationsAreFound() {
        Program program = parseProgram("label:\nconstant equ 1\nvariable set 1\nxxx macro\nendm");
        FindDeclarationsVisitor visitor = new FindDeclarationsVisitor();
        visitor.visit(program);

        NameSpace env = program.env();
        assertEquals(Optional.of(new Label(0,0, "label")), env.getDeclaration("LABEL"));
        assertEquals(Optional.of(new PseudoEqu(0,0, "constant")), env.getDeclaration("cOnStanT"));
        assertEquals(
            Optional.of(new PseudoSet(0,0, "variable")
                .addChild(new ExprNumber(0,0,1))),
            env.getDeclaration("VarIable")
        );
        assertEquals(Optional.of(new PseudoMacroDef(0,0,"xxx")), env.getMacro("XXX"));
    }

    @Test
    public void testMacrosAndDeclarationsAreIndependent() {

    }

    @Test
    public void testDeclarationAlreadyDefined() {

    }

    @Test
    public void testMacroAlreadyDefined() {

    }

}
