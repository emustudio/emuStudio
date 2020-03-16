/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.memory.standard;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class RangeTreeTest {

    @Test
    public void testListRangesContinuous() {
        RangeTree tree = new RangeTree();

        tree.add(0, 20);
        tree.add(20, 25);

        List<RangeTree.Range> ranges = tree.getRanges();

        assertEquals(1, ranges.size());
        assertEquals(0, ranges.get(0).from);
        assertEquals(25, ranges.get(0).to);
    }

    @Test
    public void testListRangesInBetween() {
        RangeTree tree = new RangeTree();

        tree.add(0, 20);
        tree.add(10, 25);

        List<RangeTree.Range> ranges = tree.getRanges();

        assertEquals(1, ranges.size());
        assertEquals(0, ranges.get(0).from);
        assertEquals(25, ranges.get(0).to);
    }


    @Test
    public void testListMultipleRanges() {
        RangeTree tree = new RangeTree();

        tree.add(0, 20);
        tree.add(25, 30);

        List<RangeTree.Range> ranges = tree.getRanges();

        assertEquals(2, ranges.size());
        assertEquals(0, ranges.get(0).from);
        assertEquals(20, ranges.get(0).to);
        assertEquals(25, ranges.get(1).from);
        assertEquals(30, ranges.get(1).to);
    }

    @Test
    public void testAddRangesRemoveInBetweenSplitsItIntoTwo() {
        RangeTree tree = new RangeTree();

        tree.add(0, 20);
        tree.remove(5, 15);

        List<RangeTree.Range> ranges = tree.getRanges();

        assertEquals(2, ranges.size());
        assertEquals(0, ranges.get(0).from);
        assertEquals(4, ranges.get(0).to);
        assertEquals(16, ranges.get(1).from);
        assertEquals(20, ranges.get(1).to);
    }

    @Test
    public void testIsInRange() {
        RangeTree tree = new RangeTree();

        tree.add(0, 20);
        for (int i = 0; i <= 20; i++) {
            assertTrue(tree.isIn(i));
        }
        assertFalse(tree.isIn(-1));
        assertFalse(tree.isIn(21));
    }
}
