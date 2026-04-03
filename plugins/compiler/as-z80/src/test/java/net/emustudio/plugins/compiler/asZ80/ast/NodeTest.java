/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.data.DataDB;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprNumber;
import net.emustudio.plugins.compiler.asZ80.ast.pseudo.PseudoLabel;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class NodeTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    @Test
    public void testAddChild() {
        DataDB parent = new DataDB(POS);
        ExprNumber child = new ExprNumber(POS, 42);
        parent.addChild(child);

        assertEquals(1, parent.getChildren().size());
        assertEquals(child, parent.getChild(0));
        assertTrue(child.getParent().isPresent());
        assertSame(parent, child.getParent().get());
    }

    @Test
    public void testAddChildFirst() {
        DataDB parent = new DataDB(POS);
        ExprNumber first = new ExprNumber(POS, 1);
        ExprNumber second = new ExprNumber(POS, 2);
        parent.addChild(second);
        parent.addChildFirst(first);

        assertEquals(2, parent.getChildren().size());
        assertEquals(first, parent.getChild(0));
        assertEquals(second, parent.getChild(1));
    }

    @Test
    public void testAddChildAt() {
        DataDB parent = new DataDB(POS);
        ExprNumber first = new ExprNumber(POS, 1);
        ExprNumber second = new ExprNumber(POS, 2);
        ExprNumber third = new ExprNumber(POS, 3);
        parent.addChild(first);
        parent.addChild(third);
        parent.addChildAt(1, second);

        assertEquals(3, parent.getChildren().size());
        assertEquals(first, parent.getChild(0));
        assertEquals(second, parent.getChild(1));
        assertEquals(third, parent.getChild(2));
    }

    @Test
    public void testAddChildren() {
        DataDB parent = new DataDB(POS);
        ExprNumber child1 = new ExprNumber(POS, 1);
        ExprNumber child2 = new ExprNumber(POS, 2);
        parent.addChildren(List.of(child1, child2));

        assertEquals(2, parent.getChildren().size());
        assertTrue(child1.getParent().isPresent());
        assertTrue(child2.getParent().isPresent());
    }

    @Test
    public void testGetChildrenReturnsUnmodifiableCopy() {
        DataDB parent = new DataDB(POS);
        parent.addChild(new ExprNumber(POS, 1));
        List<Node> children = parent.getChildren();
        try {
            children.add(new ExprNumber(POS, 2));
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testCollectChild() {
        DataDB parent = new DataDB(POS);
        ExprNumber number = new ExprNumber(POS, 42);
        parent.addChild(number);

        Optional<ExprNumber> result = parent.collectChild(ExprNumber.class);
        assertTrue(result.isPresent());
        assertEquals(number, result.get());
    }

    @Test
    public void testCollectChildNotFound() {
        DataDB parent = new DataDB(POS);
        parent.addChild(new ExprNumber(POS, 42));

        Optional<PseudoLabel> result = parent.collectChild(PseudoLabel.class);
        assertFalse(result.isPresent());
    }

    @Test
    public void testGetParentEmpty() {
        DataDB node = new DataDB(POS);
        assertFalse(node.getParent().isPresent());
    }

    @Test
    public void testRemoveChild() {
        DataDB parent = new DataDB(POS);
        ExprNumber child = new ExprNumber(POS, 42);
        parent.addChild(child);
        parent.removeChild(child);

        assertEquals(0, parent.getChildren().size());
        assertFalse(child.getParent().isPresent());
    }

    @Test
    public void testRemoveSelf() {
        DataDB parent = new DataDB(POS);
        ExprNumber child = new ExprNumber(POS, 42);
        parent.addChild(child);

        Optional<Node> result = child.remove();
        assertTrue(result.isPresent());
        assertSame(parent, result.get());
        assertEquals(0, parent.getChildren().size());
    }

    @Test
    public void testRemoveSelfNoParent() {
        DataDB node = new DataDB(POS);
        Optional<Node> result = node.remove();
        assertFalse(result.isPresent());
    }

    @Test
    public void testExclude() {
        Program program = new Program("test");
        DataDB parent = new DataDB(POS);
        ExprNumber child1 = new ExprNumber(POS, 1);
        ExprNumber child2 = new ExprNumber(POS, 2);
        parent.addChild(child1);
        parent.addChild(child2);
        program.addChild(parent);

        parent.exclude();

        assertEquals(2, program.getChildren().size());
        assertEquals(child1, program.getChild(0));
        assertEquals(child2, program.getChild(1));
    }

    @Test
    public void testExcludeNoParent() {
        DataDB node = new DataDB(POS);
        ExprNumber child = new ExprNumber(POS, 1);
        node.addChild(child);
        // Should not throw
        node.exclude();
    }

    @Test
    public void testAddressGetSet() {
        DataDB node = new DataDB(POS);
        assertEquals(0, node.getAddress());
        node.setAddress(100);
        assertEquals(100, node.getAddress());
    }

    @Test
    public void testDefaultEvalReturnsEmpty() {
        DataDB node = new DataDB(POS);
        assertFalse(node.eval(Optional.of(0), new NameSpace()).isPresent());
    }

    @Test
    public void testSetMaxValue() {
        ExprNumber node = new ExprNumber(POS, 42);
        node.setMaxValue(0xFF);
        assertTrue(node.getMaxValue().isPresent());
        assertEquals(0xFF, node.getMaxValue().get().intValue());
        assertTrue(node.getSizeBytes().isPresent());
        assertEquals(1, node.getSizeBytes().get().intValue());
    }

    @Test
    public void testSetMaxValue2Bytes() {
        ExprNumber node = new ExprNumber(POS, 42);
        node.setMaxValue(0xFFFF);
        assertTrue(node.getMaxValue().isPresent());
        assertEquals(0xFFFF, node.getMaxValue().get().intValue());
        assertTrue(node.getSizeBytes().isPresent());
        assertEquals(2, node.getSizeBytes().get().intValue());
    }

    @Test
    public void testSetSizeBytes() {
        ExprNumber node = new ExprNumber(POS, 42);
        node.setSizeBytes(2);
        assertTrue(node.getSizeBytes().isPresent());
        assertEquals(2, node.getSizeBytes().get().intValue());
        assertTrue(node.getMaxValue().isPresent());
        assertEquals(0xFFFF, node.getMaxValue().get().intValue());
    }

    @Test
    public void testSetSizeBytes1() {
        ExprNumber node = new ExprNumber(POS, 42);
        node.setSizeBytes(1);
        assertEquals(1, node.getSizeBytes().get().intValue());
        assertEquals(0xFF, node.getMaxValue().get().intValue());
    }

    @Test
    public void testCopy() {
        DataDB parent = new DataDB(POS);
        ExprNumber child = new ExprNumber(POS, 42);
        child.setMaxValue(0xFF);
        parent.addChild(child);

        Node copied = parent.copy();
        assertEquals(parent.getClass(), copied.getClass());
        assertEquals(1, copied.getChildren().size());
        assertEquals(child, copied.getChild(0));
        assertTrue(copied.getChild(0).getMaxValue().isPresent());
    }

    @Test
    public void testToString() {
        DataDB parent = new DataDB(POS);
        parent.addChild(new ExprNumber(POS, 42));
        String result = parent.toString();
        assertNotNull(result);
        assertTrue(result.contains("DataDB"));
        assertTrue(result.contains("ExprNumber"));
    }

    @Test
    public void testToStringWithSize() {
        ExprNumber node = new ExprNumber(POS, 42);
        node.setSizeBytes(1);
        String result = node.toString();
        assertTrue(result.contains("size=1"));
    }

    @Test
    public void testEqualsReflexive() {
        DataDB node = new DataDB(POS);
        assertEquals(node, node);
    }

    @Test
    public void testEqualsSameClass() {
        DataDB node1 = new DataDB(POS);
        DataDB node2 = new DataDB(POS);
        assertEquals(node1, node2);
    }

    @Test
    public void testNotEqualsNull() {
        DataDB node = new DataDB(POS);
        assertNotEquals(node, null);
    }

    @Test
    public void testNotEqualsDifferentClass() {
        DataDB db = new DataDB(POS);
        ExprNumber num = new ExprNumber(POS, 0);
        assertNotEquals(db, num);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullPosition() {
        new DataDB(null);
    }
}

