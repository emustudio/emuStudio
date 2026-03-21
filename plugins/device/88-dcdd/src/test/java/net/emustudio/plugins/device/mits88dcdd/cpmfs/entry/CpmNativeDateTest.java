/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs.entry;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.DateFormat;
import org.junit.Test;

import java.nio.ByteBuffer;

import static net.emustudio.emulib.runtime.helpers.NumberUtils.bin2bcd;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile.ENTRY_SIZE;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmNativeDate.*;
import static org.junit.Assert.*;

public class CpmNativeDateTest {

    private ByteBuffer makeNativeEntry(int d1, int d2, int d3) {
        // NATIVE format: 21 00 C1(2) M1(4) A1(4)  C2(2) M2(4) A2(4)  C3(2) M3(4) A3(4)
        ByteBuffer buf = ByteBuffer.allocate(ENTRY_SIZE);
        buf.put((byte) STATUS_DATESTAMP);
        buf.put((byte) 0); // padding
        for (int days : new int[]{d1, d2, d3}) {
            buf.put((byte) (days & 0xFF));        // create days low
            buf.put((byte) ((days >> 8) & 0xFF)); // create days high
            buf.put((byte) (days & 0xFF));        // modify days low
            buf.put((byte) ((days >> 8) & 0xFF)); // modify days high
            buf.put((byte) bin2bcd(10));           // modify hour (10)
            buf.put((byte) bin2bcd(30));           // modify minute (30)
            buf.put((byte) (days & 0xFF));        // access days low
            buf.put((byte) ((days >> 8) & 0xFF)); // access days high
            buf.put((byte) bin2bcd(12));           // access hour
            buf.put((byte) bin2bcd(45));           // access minute
        }
        buf.flip();
        return buf;
    }

    private ByteBuffer makeNative2Entry(int d1, int d2, int d3) {
        // NATIVE2 format: 21 C1(4) M1(4) 00 00  C2(4) M2(4) 00 00  C3(4) M3(4) 00 00 00
        ByteBuffer buf = ByteBuffer.allocate(ENTRY_SIZE);
        buf.put((byte) STATUS_DATESTAMP);
        for (int days : new int[]{d1, d2, d3}) {
            buf.put((byte) (days & 0xFF));
            buf.put((byte) ((days >> 8) & 0xFF));
            buf.put((byte) bin2bcd(8));
            buf.put((byte) bin2bcd(15));
            buf.put((byte) (days & 0xFF));
            buf.put((byte) ((days >> 8) & 0xFF));
            buf.put((byte) bin2bcd(20));
            buf.put((byte) bin2bcd(59));
            buf.put((byte) 0);
            buf.put((byte) 0);
        }
        // trailing byte (32nd)
        buf.put((byte) 0);
        buf.flip();
        return buf;
    }

    @Test
    public void testFromEntryNativeParsesDays() {
        CpmNativeDate nd = CpmNativeDate.fromEntry(makeNativeEntry(100, 200, 300), DateFormat.NATIVE);
        assertEquals(100, nd.datestamps[0][CREATE].days);
        assertEquals(200, nd.datestamps[1][CREATE].days);
        assertEquals(300, nd.datestamps[2][CREATE].days);
    }

    @Test
    public void testFromEntryNativeParsesModifyTime() {
        CpmNativeDate nd = CpmNativeDate.fromEntry(makeNativeEntry(100, 200, 300), DateFormat.NATIVE);
        assertEquals(10, nd.datestamps[0][MODIFY].hour);
        assertEquals(30, nd.datestamps[0][MODIFY].minute);
    }

    @Test
    public void testFromEntryNativeParsesAccessTime() {
        CpmNativeDate nd = CpmNativeDate.fromEntry(makeNativeEntry(100, 200, 300), DateFormat.NATIVE);
        assertEquals(12, nd.datestamps[0][ACCESS].hour);
        assertEquals(45, nd.datestamps[0][ACCESS].minute);
    }

    @Test
    public void testNativeCreateHasNoTime() {
        CpmNativeDate nd = CpmNativeDate.fromEntry(makeNativeEntry(100, 200, 300), DateFormat.NATIVE);
        assertEquals(0, nd.datestamps[0][CREATE].hour);
        assertEquals(0, nd.datestamps[0][CREATE].minute);
    }

    @Test
    public void testFromEntryNative2ParsesDays() {
        CpmNativeDate nd = CpmNativeDate.fromEntry(makeNative2Entry(500, 600, 700), DateFormat.NATIVE2);
        assertEquals(500, nd.datestamps[0][CREATE].days);
        assertEquals(600, nd.datestamps[1][CREATE].days);
        assertEquals(700, nd.datestamps[2][CREATE].days);
    }

    @Test
    public void testFromEntryNative2ParsesCreateTime() {
        CpmNativeDate nd = CpmNativeDate.fromEntry(makeNative2Entry(500, 600, 700), DateFormat.NATIVE2);
        assertEquals(8, nd.datestamps[0][CREATE].hour);
        assertEquals(15, nd.datestamps[0][CREATE].minute);
    }

    @Test
    public void testFromEntryNative2ParsesModifyTime() {
        CpmNativeDate nd = CpmNativeDate.fromEntry(makeNative2Entry(500, 600, 700), DateFormat.NATIVE2);
        assertEquals(20, nd.datestamps[0][MODIFY].hour);
        assertEquals(59, nd.datestamps[0][MODIFY].minute);
    }

    @Test
    public void testNative2HasNoAccessTime() {
        CpmNativeDate nd = CpmNativeDate.fromEntry(makeNative2Entry(500, 600, 700), DateFormat.NATIVE2);
        assertEquals(DateStamp.EMPTY, nd.datestamps[0][ACCESS]);
    }

    @Test
    public void testDays16BitNotTruncated() {
        // days = 0x1FF = 511 - must not be truncated to 0xFF = 255
        CpmNativeDate nd = CpmNativeDate.fromEntry(makeNativeEntry(511, 0, 0), DateFormat.NATIVE);
        assertEquals(511, nd.datestamps[0][CREATE].days);
    }

    @Test
    public void testNativeRoundTrip() {
        ByteBuffer original = makeNativeEntry(100, 200, 300);
        CpmNativeDate nd = CpmNativeDate.fromEntry(original, DateFormat.NATIVE);
        ByteBuffer serialized = nd.toEntry();

        // Re-parse and verify
        CpmNativeDate nd2 = CpmNativeDate.fromEntry(serialized, DateFormat.NATIVE);
        assertEquals(nd.datestamps[0][CREATE].days, nd2.datestamps[0][CREATE].days);
        assertEquals(nd.datestamps[1][MODIFY].hour, nd2.datestamps[1][MODIFY].hour);
        assertEquals(nd.datestamps[2][ACCESS].minute, nd2.datestamps[2][ACCESS].minute);
    }

    @Test
    public void testNative2RoundTrip() {
        ByteBuffer original = makeNative2Entry(500, 600, 700);
        CpmNativeDate nd = CpmNativeDate.fromEntry(original, DateFormat.NATIVE2);
        ByteBuffer serialized = nd.toEntry();

        CpmNativeDate nd2 = CpmNativeDate.fromEntry(serialized, DateFormat.NATIVE2);
        assertEquals(nd.datestamps[0][CREATE].days, nd2.datestamps[0][CREATE].days);
        assertEquals(nd.datestamps[1][MODIFY].minute, nd2.datestamps[1][MODIFY].minute);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromEntryInvalidStatus() {
        ByteBuffer buf = ByteBuffer.allocate(ENTRY_SIZE);
        buf.put((byte) 0x00); // not STATUS_DATESTAMP
        for (int i = 1; i < ENTRY_SIZE; i++) buf.put((byte) 0);
        buf.flip();
        CpmNativeDate.fromEntry(buf, DateFormat.NATIVE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromEntryRejectsNotUsed() {
        ByteBuffer buf = ByteBuffer.allocate(ENTRY_SIZE);
        buf.put((byte) STATUS_DATESTAMP);
        for (int i = 1; i < ENTRY_SIZE; i++) buf.put((byte) 0);
        buf.flip();
        CpmNativeDate.fromEntry(buf, DateFormat.NOT_USED);
    }
}

