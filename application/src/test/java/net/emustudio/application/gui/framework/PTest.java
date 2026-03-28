/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import net.emustudio.application.settings.SchemaPoint;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

public class PTest {

    @Test
    public void factoryAndConversionMethodsPreserveCoordinates() {
        P fromInt = P.of(10, 20);
        P fromDouble = P.of(10.7, 20.2);
        P fromSchemaPoint = P.of(SchemaPoint.of(30, 40));
        P fromPoint = P.of(new Point(50, 60));

        assertEquals(10, fromInt.ix());
        assertEquals(20, fromInt.iy());
        assertEquals(10, fromDouble.ix());
        assertEquals(20, fromDouble.iy());
        assertEquals(30, fromSchemaPoint.ix());
        assertEquals(40, fromSchemaPoint.iy());
        assertEquals(50, fromPoint.ix());
        assertEquals(60, fromPoint.iy());
        assertEquals(SchemaPoint.of(50, 60), fromPoint.toSchemaPoint());
    }

    @Test
    public void moveCopyMinusDiffAndRectangleChecksBehaveAsExpected() {
        P original = P.of(20, 30);
        P copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(20, copy.ix());
        assertEquals(30, copy.iy());

        P diff = original.diff(5, -10);
        P minus = original.minus(P.of(7, 11));

        assertEquals(25, diff.ix());
        assertEquals(20, diff.iy());
        assertEquals(13, minus.ix());
        assertEquals(19, minus.iy());

        original.move(P.of(100, 200));
        assertEquals(100, original.ix());
        assertEquals(200, original.iy());
        assertTrue(original.isInRectangle(new Point(90, 190), new Point(110, 210)));
        assertFalse(original.isInRectangle(new Point(0, 0), new Point(50, 50)));
    }
}
