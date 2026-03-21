/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.sectorops.*;
import org.junit.Test;

import java.nio.ByteBuffer;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFormat.RECORD_SIZE;
import static org.junit.Assert.*;

public class SectorOpsTest {

    private ByteBuffer makeRecord() {
        ByteBuffer record = ByteBuffer.allocate(RECORD_SIZE);
        for (int i = 0; i < RECORD_SIZE; i++) {
            record.put((byte) (i & 0xFF));
        }
        record.flip();
        return record;
    }

    private void assertRoundTrip(SectorOps ops, int track, int sector) {
        ByteBuffer record = makeRecord();
        Position pos = new Position(track, sector);
        ByteBuffer sectorBuf = ops.toSector(record, pos);
        ByteBuffer recovered = ops.toRecord(sectorBuf, pos);
        assertEquals(RECORD_SIZE, recovered.remaining());
        for (int i = 0; i < RECORD_SIZE; i++) {
            assertEquals("byte at " + i, i & 0xFF, recovered.get() & 0xFF);
        }
    }

    // ----- Altair8deramp -----

    @Test
    public void testAltair8derampSystemTrackRoundTrip() {
        assertRoundTrip(Altair8deramp.INSTANCE, 3, 5);
    }

    @Test
    public void testAltair8derampDataTrackRoundTrip() {
        assertRoundTrip(Altair8deramp.INSTANCE, 10, 7);
    }

    @Test
    public void testAltair8derampSystemTrackPrefix() {
        ByteBuffer record = makeRecord();
        Position pos = new Position(0, 0);
        ByteBuffer sector = Altair8deramp.INSTANCE.toSector(record, pos);
        assertEquals(0, sector.get(0));
        assertEquals(1, sector.get(1));
    }

    @Test
    public void testAltair8derampDataTrackPrefix() {
        ByteBuffer record = makeRecord();
        Position pos = new Position(10, 5);
        ByteBuffer sector = Altair8deramp.INSTANCE.toSector(record, pos);
        assertEquals(5, sector.get(0));
    }

    // ----- Altair8mits -----

    @Test
    public void testAltair8mitsRoundTrip() {
        assertRoundTrip(Altair8mits.INSTANCE, 10, 3);
    }

    @Test
    public void testAltair8mitsTrackAndSectorInSector() {
        ByteBuffer record = makeRecord();
        Position pos = new Position(15, 4);
        ByteBuffer sector = Altair8mits.INSTANCE.toSector(record, pos);
        assertEquals((byte) (15 | 0x80), sector.get(0));
        assertEquals((byte) ((4 * 17) % 32), sector.get(1));
    }

    // ----- AltairMinidiskDeramp -----

    @Test
    public void testAltairMinidiskDerampSystemTrackRoundTrip() {
        assertRoundTrip(AltairMinidiskDeramp.INSTANCE, 2, 0);
    }

    @Test
    public void testAltairMinidiskDerampDataTrackRoundTrip() {
        assertRoundTrip(AltairMinidiskDeramp.INSTANCE, 10, 5);
    }

    // ----- DUMMY -----

    @Test
    public void testDummySectorOpsPassThrough() {
        ByteBuffer record = makeRecord();
        Position pos = new Position(0, 0);
        assertSame(record, SectorOps.DUMMY.toSector(record, pos));
        assertSame(record, SectorOps.DUMMY.toRecord(record, pos));
    }

    // ----- fromString -----

    @Test
    public void testFromStringAltairFloppyMits() {
        assertSame(Altair8mits.INSTANCE, SectorOps.fromString("altair-floppy-mits"));
    }

    @Test
    public void testFromStringAltairFloppyDeramp() {
        assertSame(Altair8deramp.INSTANCE, SectorOps.fromString("altair-floppy-deramp"));
    }

    @Test
    public void testFromStringAltairMinidiskDeramp() {
        assertSame(AltairMinidiskDeramp.INSTANCE, SectorOps.fromString("altair-minidisk-deramp"));
    }

    @Test
    public void testFromStringUnknownReturnsDummy() {
        assertSame(SectorOps.DUMMY, SectorOps.fromString("unknown-ops"));
    }
}

