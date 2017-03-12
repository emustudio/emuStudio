/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.memory.standard.impl;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class RangeTreeTest {

    @Test
    public void testListRangesContinuous() throws Exception {
        RangeTree tree = new RangeTree();
        
        tree.add(0, 20);
        tree.add(20, 25);
        
        List<RangeTree.Range> ranges = tree.getRanges();
        
        assertEquals(1, ranges.size());
        assertEquals(0, ranges.get(0).from);
        assertEquals(25, ranges.get(0).to);
    }

    @Test
    public void testListRangesInBetween() throws Exception {
        RangeTree tree = new RangeTree();
        
        tree.add(0, 20);
        tree.add(10, 25);
        
        List<RangeTree.Range> ranges = tree.getRanges();
        
        assertEquals(1, ranges.size());
        assertEquals(0, ranges.get(0).from);
        assertEquals(25, ranges.get(0).to);
    }
    
    
    @Test
    public void testListMultipleRanges() throws Exception {
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
    public void testAddRangesRemoveInBetweenSplitsItIntoTwo() throws Exception {
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
    public void testIsInRange() throws Exception {
        RangeTree tree = new RangeTree();
        
        tree.add(0, 20);
        for (int i = 0; i <= 20; i++) {
            assertTrue(tree.isIn(i));
        }
        assertFalse(tree.isIn(-1));
        assertFalse(tree.isIn(21));
    }
    

}
