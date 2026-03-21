/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs.entry;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile.*;
import static org.junit.Assert.*;

public class CpmFileTest {

    private static List<Byte> zeroAl() {
        List<Byte> al = new ArrayList<>();
        for (int i = 0; i < RAW_BLOCK_POINTERS_COUNT; i++) {
            al.add((byte) 0);
        }
        return al;
    }

    private static List<Byte> makeAl(int... values) {
        List<Byte> al = new ArrayList<>();
        for (int v : values) {
            al.add((byte) v);
        }
        while (al.size() < RAW_BLOCK_POINTERS_COUNT) {
            al.add((byte) 0);
        }
        return al;
    }

    @Test
    public void testConstructorParsesFileNameAndExtension() {
        CpmFile f = new CpmFile((byte) 0, "test.com", 0, 0, (byte) 0, (byte) 0, (byte) 10, zeroAl());
        assertEquals("TEST", f.fileName);
        assertEquals("COM", f.fileExt);
        assertEquals("TEST.COM", f.getFileName());
    }

    @Test
    public void testConstructorWithNoExtension() {
        CpmFile f = new CpmFile((byte) 0, "MYFILE", 0, 0, (byte) 0, (byte) 0, (byte) 5, zeroAl());
        assertEquals("MYFILE", f.fileName);
        assertEquals("", f.fileExt);
        assertEquals("MYFILE", f.getFileName());
    }

    @Test
    public void testExtentNumberSplitsToExAndS2() {
        CpmFile f = new CpmFile((byte) 0, "A.B", 0, 33, (byte) 0, (byte) 0, (byte) 1, zeroAl());
        // extentNumber = 33 = 32*1 + 1
        assertEquals(1, f.ex);
        assertEquals(1, f.s2);
        assertEquals(33, f.extentNumber);
    }

    @Test
    public void testNumberOfRecordsWithExm0() {
        // exm=0: numberOfRecords = (ex & 0) * 128 + rc = rc
        CpmFile f = new CpmFile((byte) 0, "A.B", 0, 0, (byte) 0, (byte) 0, (byte) 42, zeroAl());
        assertEquals(42, f.numberOfRecords);
    }

    @Test
    public void testNumberOfRecordsWithExm1() {
        // exm=1, extentNumber=1, ex=1: numberOfRecords = (1 & 1) * 128 + rc
        CpmFile f = new CpmFile((byte) 0, "A.B", 0, 1, (byte) 1, (byte) 0, (byte) 10, zeroAl());
        assertEquals(128 + 10, f.numberOfRecords);
    }

    @Test
    public void testFromEntryRoundTrip() {
        // Build a raw 32-byte entry: status=0, name="HELLO   COM", ex=2, bc=10, s2=0, rc=50, 16 zero al bytes
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put((byte) 0); // status
        entry.put("HELLO   COM".getBytes()); // 11 bytes name+ext
        entry.put((byte) 2);  // ex
        entry.put((byte) 10); // bc
        entry.put((byte) 0);  // s2
        entry.put((byte) 50); // rc
        for (int i = 0; i < 16; i++) entry.put((byte) 0);
        entry.flip();

        CpmFile f = CpmFile.fromEntry(entry, (byte) 0);
        assertEquals(0, f.status);
        assertEquals("HELLO", f.fileName.trim());
        assertEquals("COM", f.fileExt.trim());
        assertEquals(2, f.ex);
        assertEquals(10, f.bc);
        assertEquals(0, f.s2);
        assertEquals(50, f.rc);
    }

    @Test
    public void testToEntryPreservesFields() {
        CpmFile f = new CpmFile((byte) 0, "HELLO.COM", 0, 0, (byte) 0, (byte) 0, (byte) 10, makeAl(5, 6));
        ByteBuffer entry = f.toEntry();
        CpmFile f2 = CpmFile.fromEntry(entry, (byte) 0);
        assertEquals(f.getFileName(), f2.getFileName());
        assertEquals(f.ex, f2.ex);
        assertEquals(f.rc, f2.rc);
        assertEquals(f.bc, f2.bc);
    }

    @Test
    public void testToEntryWithNewStatus() {
        CpmFile f = new CpmFile((byte) 0, "X.Y", 0, 0, (byte) 0, (byte) 0, (byte) 1, zeroAl());
        ByteBuffer entry = f.toEntry((byte) 0xE5);
        assertEquals((byte) 0xE5, entry.get(0));
    }

    @Test
    public void testFlagsFromEntryReadOnly() {
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put((byte) 0); // status
        // name bytes: set bit 7 on byte 8 (E0 = read-only flag)
        byte[] name = "HELLO   COM".getBytes();
        name[FLAG_READ_ONLY] |= (byte) 0x80; // set read-only
        entry.put(name);
        entry.put((byte) 0); // ex
        entry.put((byte) 0); // bc
        entry.put((byte) 0); // s2
        entry.put((byte) 1); // rc
        for (int i = 0; i < 16; i++) entry.put((byte) 0);
        entry.flip();

        CpmFile f = CpmFile.fromEntry(entry, (byte) 0);
        assertTrue((f.flags & (1 << FLAG_READ_ONLY)) != 0);
        assertTrue(f.getFlagsString().contains("R"));
    }

    @Test
    public void testFlagsRoundTrip() {
        int flags = (1 << FLAG_READ_ONLY) | (1 << FLAG_ARCHIVED);
        CpmFile f = new CpmFile((byte) 0, "F.TXT", flags, 0, (byte) 0, (byte) 0, (byte) 1, zeroAl());
        ByteBuffer entry = f.toEntry();
        CpmFile f2 = CpmFile.fromEntry(entry, (byte) 0);
        assertTrue((f2.flags & (1 << FLAG_READ_ONLY)) != 0);
        assertTrue((f2.flags & (1 << FLAG_ARCHIVED)) != 0);
    }

    @Test
    public void testFlagsDoNotOverlap() {
        // Verify that FLAG_ARCHIVED (10) and FLAG_READ_ONLY (8) | FLAG_DATE_STAMP (2) don't overlap
        int roDs = (1 << FLAG_READ_ONLY) | (1 << FLAG_DATE_STAMP);
        int ar = (1 << FLAG_ARCHIVED);
        assertNotEquals(roDs, ar);
    }

    @Test
    public void testExtensionWrittenCorrectlyInToEntry() {
        CpmFile f = new CpmFile((byte) 0, "AB.XYZ", 0, 0, (byte) 0, (byte) 0, (byte) 1, zeroAl());
        ByteBuffer entry = f.toEntry();
        CpmFile f2 = CpmFile.fromEntry(entry, (byte) 0);
        assertEquals("AB.XYZ", f2.getFileName());
    }

    @Test
    public void testBlockPointers() {
        List<Byte> al = makeAl(0x02, 0x03, 0x04);
        CpmFile f = new CpmFile((byte) 0, "A.B", 0, 0, (byte) 0, (byte) 0, (byte) 1, al);
        assertEquals((byte) 0x02, (byte) f.al.get(0));
        assertEquals((byte) 0x03, (byte) f.al.get(1));
        assertEquals((byte) 0x04, (byte) f.al.get(2));
    }

    @Test
    public void testEntryNumber() {
        // entryNumber = ((32*s2)+ex) / (exm+1)
        // extentNumber=33 => ex=1, s2=1 => entryNumber = (32+1)/(0+1) = 33
        CpmFile f = new CpmFile((byte) 0, "A.B", 0, 33, (byte) 0, (byte) 0, (byte) 1, zeroAl());
        assertEquals(33, f.entryNumber);
    }
}

