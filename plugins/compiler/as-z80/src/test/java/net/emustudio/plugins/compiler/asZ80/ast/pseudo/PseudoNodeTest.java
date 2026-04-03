/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.pseudo;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Evaluated;
import net.emustudio.plugins.compiler.asZ80.ast.NameSpace;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class PseudoNodeTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    // PseudoLabel tests

    @Test
    public void testLabelEvalWithAddress() {
        PseudoLabel label = new PseudoLabel(POS, "loop");
        Optional<Evaluated> result = label.eval(Optional.of(100), new NameSpace());
        assertTrue(result.isPresent());
        assertEquals(100, result.get().value);
        assertTrue(result.get().isAddress);
    }

    @Test
    public void testLabelEvalWithoutAddress() {
        PseudoLabel label = new PseudoLabel(POS, "loop");
        Optional<Evaluated> result = label.eval(Optional.empty(), new NameSpace());
        assertFalse(result.isPresent());
    }

    @Test
    public void testLabelEqualsSame() {
        PseudoLabel l1 = new PseudoLabel(POS, "loop");
        PseudoLabel l2 = new PseudoLabel(POS, "loop");
        assertEquals(l1, l2);
    }

    @Test
    public void testLabelNotEqualsDifferent() {
        PseudoLabel l1 = new PseudoLabel(POS, "loop");
        PseudoLabel l2 = new PseudoLabel(POS, "end");
        assertNotEquals(l1, l2);
    }

    @Test
    public void testLabelNotEqualsNull() {
        PseudoLabel label = new PseudoLabel(POS, "loop");
        assertNotEquals(label, null);
    }

    @Test
    public void testLabelToString() {
        PseudoLabel label = new PseudoLabel(POS, "loop");
        assertTrue(label.toString().contains("Label(loop)"));
    }

    @Test
    public void testLabelCopy() {
        PseudoLabel original = new PseudoLabel(POS, "loop");
        Node copy = original.copy();
        assertTrue(copy instanceof PseudoLabel);
        assertEquals(original, copy);
    }

    @Test(expected = NullPointerException.class)
    public void testLabelNullThrows() {
        new PseudoLabel(POS, null);
    }

    // PseudoEqu tests

    @Test
    public void testEquEqualsSame() {
        PseudoEqu e1 = new PseudoEqu(POS, "CONST");
        PseudoEqu e2 = new PseudoEqu(POS, "CONST");
        assertEquals(e1, e2);
    }

    @Test
    public void testEquNotEqualsDifferent() {
        PseudoEqu e1 = new PseudoEqu(POS, "A");
        PseudoEqu e2 = new PseudoEqu(POS, "B");
        assertNotEquals(e1, e2);
    }

    @Test
    public void testEquNotEqualsNull() {
        PseudoEqu e = new PseudoEqu(POS, "A");
        assertNotEquals(e, null);
    }

    @Test
    public void testEquToString() {
        PseudoEqu e = new PseudoEqu(POS, "CONST");
        assertTrue(e.toString().contains("PseudoEqu(CONST)"));
    }

    @Test
    public void testEquCopy() {
        PseudoEqu original = new PseudoEqu(POS, "X");
        Node copy = original.copy();
        assertTrue(copy instanceof PseudoEqu);
        assertEquals(original, copy);
    }

    @Test(expected = NullPointerException.class)
    public void testEquNullIdThrows() {
        new PseudoEqu(POS, null);
    }

    // PseudoVar tests

    @Test
    public void testVarEqualsSame() {
        PseudoVar v1 = new PseudoVar(POS, "counter");
        PseudoVar v2 = new PseudoVar(POS, "counter");
        assertEquals(v1, v2);
    }

    @Test
    public void testVarNotEqualsDifferent() {
        PseudoVar v1 = new PseudoVar(POS, "a");
        PseudoVar v2 = new PseudoVar(POS, "b");
        assertNotEquals(v1, v2);
    }

    @Test
    public void testVarNotEqualsNull() {
        PseudoVar v = new PseudoVar(POS, "a");
        assertNotEquals(v, null);
    }

    @Test
    public void testVarToString() {
        PseudoVar v = new PseudoVar(POS, "counter");
        assertTrue(v.toString().contains("PseudoVar(counter)"));
    }

    @Test
    public void testVarCopy() {
        PseudoVar original = new PseudoVar(POS, "x");
        Node copy = original.copy();
        assertTrue(copy instanceof PseudoVar);
        assertEquals(original, copy);
    }

    @Test(expected = NullPointerException.class)
    public void testVarNullIdThrows() {
        new PseudoVar(POS, null);
    }

    // PseudoInclude tests

    @Test
    public void testIncludeEqualsSame() {
        PseudoInclude i1 = new PseudoInclude(POS, "file.asm");
        PseudoInclude i2 = new PseudoInclude(POS, "file.asm");
        assertEquals(i1, i2);
    }

    @Test
    public void testIncludeNotEqualsDifferent() {
        PseudoInclude i1 = new PseudoInclude(POS, "a.asm");
        PseudoInclude i2 = new PseudoInclude(POS, "b.asm");
        assertNotEquals(i1, i2);
    }

    @Test
    public void testIncludeNotEqualsNull() {
        PseudoInclude inc = new PseudoInclude(POS, "file.asm");
        assertNotEquals(inc, null);
    }

    @Test
    public void testIncludeToString() {
        PseudoInclude inc = new PseudoInclude(POS, "file.asm");
        assertTrue(inc.toString().contains("PseudoInclude('file.asm')"));
    }

    @Test
    public void testIncludeCopy() {
        PseudoInclude original = new PseudoInclude(POS, "file.asm");
        Node copy = original.copy();
        assertTrue(copy instanceof PseudoInclude);
        assertEquals(original, copy);
    }

    @Test(expected = NullPointerException.class)
    public void testIncludeNullFilenameThrows() {
        new PseudoInclude(POS, null);
    }

    // PseudoMacroDef tests

    @Test
    public void testMacroDefEqualsSame() {
        PseudoMacroDef m1 = new PseudoMacroDef(POS, "mymacro");
        PseudoMacroDef m2 = new PseudoMacroDef(POS, "mymacro");
        assertEquals(m1, m2);
    }

    @Test
    public void testMacroDefNotEqualsDifferent() {
        PseudoMacroDef m1 = new PseudoMacroDef(POS, "a");
        PseudoMacroDef m2 = new PseudoMacroDef(POS, "b");
        assertNotEquals(m1, m2);
    }

    @Test
    public void testMacroDefNotEqualsNull() {
        PseudoMacroDef m = new PseudoMacroDef(POS, "a");
        assertNotEquals(m, null);
    }

    @Test
    public void testMacroDefToString() {
        PseudoMacroDef m = new PseudoMacroDef(POS, "mymacro");
        assertTrue(m.toString().contains("PseudoMacroDef(mymacro)"));
    }

    @Test
    public void testMacroDefCopy() {
        PseudoMacroDef original = new PseudoMacroDef(POS, "x");
        Node copy = original.mkCopy();
        assertTrue(copy instanceof PseudoMacroDef);
        assertEquals(original, copy);
    }

    @Test(expected = NullPointerException.class)
    public void testMacroDefNullIdThrows() {
        new PseudoMacroDef(POS, null);
    }

    // PseudoMacroCall tests

    @Test
    public void testMacroCallEqualsSame() {
        PseudoMacroCall c1 = new PseudoMacroCall(POS, "call");
        PseudoMacroCall c2 = new PseudoMacroCall(POS, "call");
        assertEquals(c1, c2);
    }

    @Test
    public void testMacroCallNotEqualsDifferent() {
        PseudoMacroCall c1 = new PseudoMacroCall(POS, "a");
        PseudoMacroCall c2 = new PseudoMacroCall(POS, "b");
        assertNotEquals(c1, c2);
    }

    @Test
    public void testMacroCallNotEqualsNull() {
        PseudoMacroCall c = new PseudoMacroCall(POS, "a");
        assertNotEquals(c, null);
    }

    @Test
    public void testMacroCallToString() {
        PseudoMacroCall c = new PseudoMacroCall(POS, "mymacro");
        assertTrue(c.toString().contains("PseudoMacroCall(mymacro)"));
    }

    @Test
    public void testMacroCallCopy() {
        PseudoMacroCall original = new PseudoMacroCall(POS, "x");
        Node copy = original.copy();
        assertTrue(copy instanceof PseudoMacroCall);
        assertEquals(original, copy);
    }

    @Test(expected = NullPointerException.class)
    public void testMacroCallNullIdThrows() {
        new PseudoMacroCall(POS, null);
    }

    // PseudoOrg tests

    @Test
    public void testOrgCopy() {
        PseudoOrg original = new PseudoOrg(POS);
        Node copy = original.copy();
        assertTrue(copy instanceof PseudoOrg);
    }

    // PseudoIf tests

    @Test
    public void testIfCopy() {
        PseudoIf original = new PseudoIf(POS);
        Node copy = original.copy();
        assertTrue(copy instanceof PseudoIf);
    }

    // PseudoIfExpression tests

    @Test
    public void testIfExpressionCopy() {
        PseudoIfExpression original = new PseudoIfExpression(POS);
        Node copy = original.copy();
        assertTrue(copy instanceof PseudoIfExpression);
    }

    // PseudoMacroArgument tests

    @Test
    public void testMacroArgumentCopy() {
        PseudoMacroArgument original = new PseudoMacroArgument(POS);
        Node copy = original.copy();
        assertTrue(copy instanceof PseudoMacroArgument);
    }

    // PseudoMacroParameter tests

    @Test
    public void testMacroParameterCopy() {
        PseudoMacroParameter original = new PseudoMacroParameter(POS);
        Node copy = original.copy();
        assertTrue(copy instanceof PseudoMacroParameter);
    }
}

