/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs.entry;

import org.junit.Test;

import java.nio.ByteBuffer;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile.ENTRY_SIZE;
import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmPlusDiscLabel.*;
import static org.junit.Assert.*;

public class CpmPlusDiscLabelTest {

    @Test
    public void testFromEntryParsesLabel() {
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put(STATUS_LABEL);
        entry.put("TESTLABEL  ".getBytes()); // 11 bytes
        entry.put((byte) 0x31); // mode
        entry.put((byte) 0x42); // password decode
        entry.put((byte) 0); // reserved
        entry.put((byte) 0); // reserved
        for (int i = 0; i < 8; i++) entry.put((byte) 0); // password
        for (int i = 0; i < 8; i++) entry.put((byte) 0); // timestamps
        entry.flip();

        CpmPlusDiscLabel label = CpmPlusDiscLabel.fromEntry(entry);
        assertEquals("TESTLABEL  ", label.label);
        assertEquals(0x31, label.mode);
        assertEquals(0x42, label.passwordDecodeByte);
    }

    @Test
    public void testFromEntryParsesTimestamps() {
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put(STATUS_LABEL);
        entry.put("MYLABEL    ".getBytes());
        entry.put((byte) (MODE_LABEL_EXISTS | MODE_TIMESTAMPS_ON_CREATION));
        entry.put((byte) 0);
        entry.put((byte) 0); // reserved
        entry.put((byte) 0);
        for (int i = 0; i < 8; i++) entry.put((byte) 0); // password

        // create stamp: days=100 (0x64), hour=10h, minute=30h (BCD)
        entry.put((byte) 0x64);
        entry.put((byte) 0x00);
        entry.put((byte) 0x10); // 10 BCD
        entry.put((byte) 0x30); // 30 BCD

        // modify stamp: days=200 (0xC8), hour=15h, minute=45h
        entry.put((byte) 0xC8);
        entry.put((byte) 0x00);
        entry.put((byte) 0x15);
        entry.put((byte) 0x45);
        entry.flip();

        CpmPlusDiscLabel label = CpmPlusDiscLabel.fromEntry(entry);
        assertEquals(100, label.createStamp.days);
        assertEquals(10, label.createStamp.hour);
        assertEquals(30, label.createStamp.minute);
        assertEquals(200, label.modifyStamp.days);
        assertEquals(15, label.modifyStamp.hour);
        assertEquals(45, label.modifyStamp.minute);
    }

    @Test
    public void testRoundTrip() {
        CpmPlusDiscLabel original = new CpmPlusDiscLabel(
                "TESTLABEL  ", (byte) (MODE_LABEL_EXISTS | MODE_PASSWORD_PROTECTION), (byte) 0x55,
                new byte[]{1, 2, 3, 4, 5, 6, 7, 8},
                new DateStamp(100, 10, 30),
                new DateStamp(200, 15, 45)
        );
        ByteBuffer entry = original.toEntry();
        CpmPlusDiscLabel parsed = CpmPlusDiscLabel.fromEntry(entry);

        assertEquals(original.label, parsed.label);
        assertEquals(original.mode, parsed.mode);
        assertEquals(original.passwordDecodeByte, parsed.passwordDecodeByte);
        assertEquals(100, parsed.createStamp.days);
        assertEquals(200, parsed.modifyStamp.days);
    }

    @Test
    public void testModeFlags() {
        byte mode = (byte) (MODE_LABEL_EXISTS | MODE_TIMESTAMPS_ON_MODIFICATION | MODE_PASSWORD_PROTECTION);
        CpmPlusDiscLabel label = new CpmPlusDiscLabel("TEST       ", mode, (byte) 0,
                new byte[8], DateStamp.EMPTY, DateStamp.EMPTY);
        String str = label.toString();
        assertTrue(str.contains("[label]"));
        assertTrue(str.contains("[modify-ts]"));
        assertTrue(str.contains("[password]"));
        assertFalse(str.contains("[create-ts]"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromEntryInvalidStatus() {
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put((byte) 0x00); // not STATUS_LABEL
        for (int i = 1; i < ENTRY_SIZE; i++) entry.put((byte) 0);
        entry.flip();
        CpmPlusDiscLabel.fromEntry(entry);
    }
}

