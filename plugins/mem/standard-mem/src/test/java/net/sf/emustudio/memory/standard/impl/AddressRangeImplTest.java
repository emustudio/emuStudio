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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AddressRangeImplTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeStartAddressThrows() throws Exception {
        new AddressRangeImpl(-1,10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeStopThrows() throws Exception {
        new AddressRangeImpl(0,-10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStopIsLesserThanStartThrows() throws Exception {
        new AddressRangeImpl(100,90);
    }

    @Test
    public void testGetters() throws Exception {
        AddressRangeImpl addressRange = new AddressRangeImpl(0, 100);
        assertEquals(0, addressRange.getStartAddress());
        assertEquals(100, addressRange.getStopAddress());
    }

    @Test
    public void testCompareTwoRanges() throws Exception {
        AddressRangeImpl range1 = new AddressRangeImpl(10,100);
        AddressRangeImpl range2 = new AddressRangeImpl(0,10);
        AddressRangeImpl range3 = new AddressRangeImpl(100,110);

        assertEquals(0, range3.compareTo(range3));
        assertEquals(1, range3.compareTo(range2));
        assertEquals(-1, range1.compareTo(range3));
    }
}
