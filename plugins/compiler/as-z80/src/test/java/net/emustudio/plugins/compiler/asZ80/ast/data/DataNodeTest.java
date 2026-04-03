/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.compiler.asZ80.ast.data;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.plugins.compiler.asZ80.ast.Node;
import net.emustudio.plugins.compiler.asZ80.ast.expr.ExprNumber;
import org.junit.Test;

import static org.junit.Assert.*;

public class DataNodeTest {
    private static final SourceCodePosition POS = new SourceCodePosition(0, 0, "test");

    // DataDB tests

    @Test
    public void testDataDBCopy() {
        DataDB original = new DataDB(POS);
        original.addChild(new ExprNumber(POS, 42));
        Node copy = original.copy();
        assertTrue(copy instanceof DataDB);
        assertEquals(1, copy.getChildren().size());
    }

    @Test
    public void testDataDBEquality() {
        DataDB d1 = new DataDB(POS);
        DataDB d2 = new DataDB(POS);
        assertEquals(d1, d2);
    }

    @Test
    public void testDataDBNotEqualsToDifferentClass() {
        DataDB db = new DataDB(POS);
        DataDW dw = new DataDW(POS);
        assertNotEquals(db, dw);
    }

    // DataDW tests

    @Test
    public void testDataDWCopy() {
        DataDW original = new DataDW(POS);
        original.addChild(new ExprNumber(POS, 1000));
        Node copy = original.copy();
        assertTrue(copy instanceof DataDW);
        assertEquals(1, copy.getChildren().size());
    }

    @Test
    public void testDataDWEquality() {
        DataDW d1 = new DataDW(POS);
        DataDW d2 = new DataDW(POS);
        assertEquals(d1, d2);
    }

    // DataDS tests

    @Test
    public void testDataDSCopy() {
        DataDS original = new DataDS(POS);
        original.addChild(new ExprNumber(POS, 10));
        Node copy = original.copy();
        assertTrue(copy instanceof DataDS);
        assertEquals(1, copy.getChildren().size());
    }

    @Test
    public void testDataDSEquality() {
        DataDS d1 = new DataDS(POS);
        DataDS d2 = new DataDS(POS);
        assertEquals(d1, d2);
    }

    @Test
    public void testDataDSNotEqualsDB() {
        DataDS ds = new DataDS(POS);
        DataDB db = new DataDB(POS);
        assertNotEquals(ds, db);
    }

    @Test
    public void testDataDBWithMultipleChildren() {
        DataDB db = new DataDB(POS);
        db.addChild(new ExprNumber(POS, 1));
        db.addChild(new ExprNumber(POS, 2));
        db.addChild(new ExprNumber(POS, 3));
        Node copy = db.copy();
        assertEquals(3, copy.getChildren().size());
    }

    @Test
    public void testDataDWWithMultipleChildren() {
        DataDW dw = new DataDW(POS);
        dw.addChild(new ExprNumber(POS, 0x1234));
        dw.addChild(new ExprNumber(POS, 0x5678));
        Node copy = dw.copy();
        assertEquals(2, copy.getChildren().size());
    }
}

