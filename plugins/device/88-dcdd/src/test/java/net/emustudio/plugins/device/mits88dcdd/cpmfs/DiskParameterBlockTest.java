/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import org.junit.Test;

import static org.junit.Assert.*;

public class DiskParameterBlockTest {

    @Test
    public void testFromBSH_bsh3() {
        // bsh=3 => blm = (1<<3)-1 = 7, block size = 128*8 = 1024
        DiskParameterBlock dpb = DiskParameterBlock.fromBSH(32, 32, 3, 242, 63, 0xC0, 0, 2);
        assertEquals(3, dpb.bsh);
        assertEquals(7, dpb.blm);
        assertEquals(0, dpb.exm); // dsm<=255, exm = (1<<(3-3))-1 = 0
    }

    @Test
    public void testFromBSH_bsh4_smallDsm() {
        // bsh=4 => blm=15, dsm<=255 => exm = (1<<(4-3))-1 = 1
        DiskParameterBlock dpb = DiskParameterBlock.fromBSH(32, 32, 4, 149, 63, 0xC0, 0, 2);
        assertEquals(4, dpb.bsh);
        assertEquals(15, dpb.blm);
        assertEquals(1, dpb.exm);
    }

    @Test
    public void testFromBSH_bsh4_largeDsm() {
        // bsh=4, dsm>255 => exm = (1<<(4-4))-1 = 0
        DiskParameterBlock dpb = DiskParameterBlock.fromBSH(32, 32, 4, 0x07F9, 0x03FF, 0xF0, 0, 6);
        assertEquals(4, dpb.bsh);
        assertEquals(0, dpb.exm);
    }

    @Test
    public void testFromBLM() {
        // blm=7 => bsh = log2(8) = 3
        DiskParameterBlock dpb = DiskParameterBlock.fromBLM(32, 32, 7, 242, 63, 0xC0, 0, 2);
        assertEquals(3, dpb.bsh);
        assertEquals(7, dpb.blm);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidBsh_tooLow() {
        DiskParameterBlock.fromBSH(32, 32, 2, 100, 63, 0xC0, 0, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidBsh_tooHigh() {
        DiskParameterBlock.fromBSH(32, 32, 8, 100, 63, 0xC0, 0, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBsh3WithLargeDsm() {
        // bsh=3 and dsm>255 is not allowed
        DiskParameterBlock.fromBSH(32, 32, 3, 256, 63, 0xC0, 0, 2);
    }

    @Test
    public void testFieldValues() {
        DiskParameterBlock dpb = new DiskParameterBlock(32, 26, 3, 7, 242, 63, 0xC0, 0, 2);
        assertEquals(32, dpb.driveSpt);
        assertEquals(26, dpb.spt);
        assertEquals(242, dpb.dsm);
        assertEquals(63, dpb.drm);
        assertEquals(0xC0, dpb.al0);
        assertEquals(0, dpb.al1);
        assertEquals(2, dpb.ofs);
    }
}

