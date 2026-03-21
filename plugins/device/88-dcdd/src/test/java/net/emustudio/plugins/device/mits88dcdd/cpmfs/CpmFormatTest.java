/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.sectorops.SectorOps;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class CpmFormatTest {

    // CP/M 1.4 SIMH: bsh=3, dsm=242, drm=63, al0=0xC0, al1=0, ofs=2, spt=26
    private CpmFormat makeCpm1Format() {
        DiskParameterBlock dpb = DiskParameterBlock.fromBSH(32, 26, 3, 242, 63, 0xC0, 0, 2);
        return new CpmFormat("cpm1-simh", dpb, 137, Optional.of(6), Optional.empty(),
                SectorOps.DUMMY, false, DateFormat.NOT_USED);
    }

    // CP/M 3 SIMH: bsh=4, dsm=0x07F9, drm=0x03FF, al0=0xF0, al1=0, ofs=6, spt=32
    private CpmFormat makeCpm3Format() {
        DiskParameterBlock dpb = DiskParameterBlock.fromBSH(32, 32, 4, 0x07F9, 0x03FF, 0xF0, 0, 6);
        return new CpmFormat("cpm3-simh", dpb, 137, Optional.of(17), Optional.empty(),
                SectorOps.DUMMY, false, DateFormat.NOT_USED);
    }

    // CP/M 2 SIMH: bsh=3, dsm=254, drm=255, al0=0xFF, al1=0, ofs=6, spt=32
    private CpmFormat makeCpm2Format() {
        DiskParameterBlock dpb = DiskParameterBlock.fromBSH(32, 32, 3, 254, 255, 0xFF, 0, 6);
        return new CpmFormat("cpm2-simh", dpb, 137, Optional.of(17), Optional.empty(),
                SectorOps.DUMMY, false, DateFormat.NOT_USED);
    }

    @Test
    public void testBlockSizeCpm1() {
        CpmFormat fmt = makeCpm1Format();
        // bsh=3 => blm=7 => blockSize = 128 * 8 = 1024
        assertEquals(1024, fmt.blockSize);
    }

    @Test
    public void testBlockSizeCpm3() {
        CpmFormat fmt = makeCpm3Format();
        // bsh=4 => blm=15 => blockSize = 128 * 16 = 2048
        assertEquals(2048, fmt.blockSize);
    }

    @Test
    public void testDirectoryBlocksAl0Only() {
        CpmFormat fmt = makeCpm1Format();
        // al0=0xC0, al1=0 => bits 7,6 set => blocks 0,1
        assertEquals(List.of(0, 1), fmt.directoryBlocks);
    }

    @Test
    public void testDirectoryBlocksAl0_0xFF() {
        CpmFormat fmt = makeCpm2Format();
        // al0=0xFF, al1=0 => blocks 0-7
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7), fmt.directoryBlocks);
    }

    @Test
    public void testDirectoryBlocksAl1Included() {
        // al0=0x80, al1=0x01 => block 0 and block 15
        DiskParameterBlock dpb = DiskParameterBlock.fromBSH(32, 32, 3, 242, 63, 0x80, 0x01, 2);
        CpmFormat fmt = new CpmFormat("test", dpb, 137, Optional.of(1), Optional.empty(),
                SectorOps.DUMMY, false, DateFormat.NOT_USED);
        assertTrue(fmt.directoryBlocks.contains(0));
        assertTrue(fmt.directoryBlocks.contains(15));
        assertEquals(2, fmt.directoryBlocks.size());
    }

    @Test
    public void testBlockPointerIsWord() {
        CpmFormat cpm1 = makeCpm1Format(); // dsm=242 <= 255
        assertFalse(cpm1.blockPointerIsWord);

        CpmFormat cpm3 = makeCpm3Format(); // dsm=0x07F9 > 255
        assertTrue(cpm3.blockPointerIsWord);
    }

    @Test
    public void testBlockPointersCountByte() {
        CpmFormat fmt = makeCpm1Format();
        assertFalse(fmt.blockPointerIsWord);
        assertEquals(16, fmt.blockPointersCount);
    }

    @Test
    public void testBlockPointersCountWord() {
        CpmFormat fmt = makeCpm3Format();
        assertTrue(fmt.blockPointerIsWord);
        assertEquals(8, fmt.blockPointersCount);
    }

    @Test
    public void testTracksCalculation() {
        CpmFormat fmt = makeCpm1Format();
        // tracks = (dsm+1) * blockSize / (spt * 128) + ofs
        // = 243 * 1024 / (26 * 128) + 2 = 243 * 1024 / 3328 + 2 = 74 + 2 = 76
        int expected = (242 + 1) * 1024 / (26 * 128) + 2;
        assertEquals(expected, fmt.tracks);
    }

    @Test
    public void testRecordsPerBlock() {
        CpmFormat fmt = makeCpm1Format();
        assertEquals(8, fmt.recordsPerBlock); // blm+1 = 8
    }

    @Test
    public void testEntriesPerBlock() {
        CpmFormat fmt = makeCpm1Format();
        assertEquals(1024 / 32, fmt.entriesPerBlock); // 32
    }

    @Test
    public void testBlockToPositionBlock0() {
        CpmFormat fmt = makeCpm1Format();
        Position pos = fmt.blockToPosition(0);
        // linearSector = 0 * 8 + 26 * 2 = 52
        // track = 52 / 26 = 2, sector = 52 % 26 = 0
        assertEquals(2, pos.track);
        assertEquals(0, pos.sector);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBlockToPositionBlockTooLarge() {
        CpmFormat fmt = makeCpm1Format();
        fmt.blockToPosition(fmt.dpb.dsm + 1);
    }

    @Test
    public void testPositionToOffset() {
        CpmFormat fmt = makeCpm1Format();
        Position pos = new Position(0, 0);
        long offset = fmt.positionToOffset(pos);
        // track=0, sector=0, skewTable[0]=0 => offset = 0
        assertEquals(0, offset);
    }

    @Test
    public void testSectorSkewTableComputed() {
        CpmFormat fmt = makeCpm1Format(); // sectorSkew=6
        assertNotNull(fmt.sectorSkewTable);
        assertEquals(26, fmt.sectorSkewTable.length); // spt=26
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSectorSkewAndTableCannotBothBePresent() {
        DiskParameterBlock dpb = DiskParameterBlock.fromBSH(32, 4, 3, 100, 63, 0xC0, 0, 2);
        new CpmFormat("test", dpb, 137, Optional.of(6), Optional.of(List.of(0, 1, 2, 3)),
                SectorOps.DUMMY, false, DateFormat.NOT_USED);
    }

    @Test
    public void testCustomSectorSkewTable() {
        DiskParameterBlock dpb = DiskParameterBlock.fromBSH(32, 4, 3, 100, 63, 0xC0, 0, 2);
        CpmFormat fmt = new CpmFormat("test", dpb, 137, Optional.empty(), Optional.of(List.of(0, 2, 1, 3)),
                SectorOps.DUMMY, false, DateFormat.NOT_USED);
        assertArrayEquals(new int[]{0, 2, 1, 3}, fmt.sectorSkewTable);
    }

    @Test
    public void testEntriesPerRecord() {
        assertEquals(4, CpmFormat.ENTRIES_PER_RECORD); // 128 / 32 = 4
    }
}

