/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import org.junit.Test;

import static org.junit.Assert.*;

public class PositionTest {

    @Test
    public void testInitialValues() {
        Position pos = new Position(5, 10);
        assertEquals(5, pos.track);
        assertEquals(10, pos.sector);
    }

    @Test
    public void testNextIncrementsSector() {
        Position pos = new Position(0, 0);
        pos.next(16);
        assertEquals(0, pos.track);
        assertEquals(1, pos.sector);
    }

    @Test
    public void testNextWrapsToNextTrack() {
        Position pos = new Position(0, 15);
        pos.next(16);
        assertEquals(1, pos.track);
        assertEquals(0, pos.sector);
    }

    @Test
    public void testNextMultipleWraps() {
        Position pos = new Position(0, 0);
        for (int i = 0; i < 48; i++) {
            pos.next(16);
        }
        assertEquals(3, pos.track);
        assertEquals(0, pos.sector);
    }

    @Test
    public void testToString() {
        Position pos = new Position(3, 7);
        assertEquals("T=3 S=7", pos.toString());
    }
}

