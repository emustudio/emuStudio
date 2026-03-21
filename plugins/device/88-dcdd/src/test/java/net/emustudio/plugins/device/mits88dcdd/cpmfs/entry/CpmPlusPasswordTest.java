/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cpmfs.entry;

import org.junit.Test;

import java.nio.ByteBuffer;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile.ENTRY_SIZE;
import static org.junit.Assert.*;

public class CpmPlusPasswordTest {

    @Test
    public void testFromEntryParsesAll() {
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put((byte) 0x10); // status (user 0 + 16)
        entry.put("HELLO   COM".getBytes()); // 11 bytes
        entry.put((byte) 0xE0); // mode (read+write+delete)
        entry.put((byte) 0x55); // password decode byte
        entry.put((byte) 0); // reserved
        entry.put((byte) 0); // reserved
        entry.put("MYPASSWD".getBytes()); // 8 bytes
        entry.flip();

        CpmPlusPassword pw = CpmPlusPassword.fromEntry(entry);
        assertEquals(0x10, pw.status);
        assertEquals("HELLO.COM", pw.getFileName());
        assertEquals((byte) 0xE0, pw.mode);
        assertEquals((byte) 0x55, pw.passwordDecodeByte);
        assertEquals("MYPASSWD", pw.passwordString);
    }

    @Test
    public void testRoundTrip() {
        CpmPlusPassword original = new CpmPlusPassword(
                (byte) 0x12, "TESTFILE", "TXT",
                (byte) 0x80, (byte) 0x33, "PASS1234".getBytes()
        );
        ByteBuffer entry = original.toEntry();
        CpmPlusPassword parsed = CpmPlusPassword.fromEntry(entry);

        assertEquals(original.status, parsed.status);
        assertEquals(original.getFileName(), parsed.getFileName());
        assertEquals(original.mode, parsed.mode);
        assertEquals(original.passwordDecodeByte, parsed.passwordDecodeByte);
        assertEquals(original.passwordString, parsed.passwordString);
    }

    @Test
    public void testExtensionRoundTrip() {
        CpmPlusPassword pw = new CpmPlusPassword(
                (byte) 0x10, "AB      ", "XYZ",
                (byte) 0, (byte) 0, new byte[8]
        );
        ByteBuffer entry = pw.toEntry();
        CpmPlusPassword parsed = CpmPlusPassword.fromEntry(entry);
        assertEquals("XYZ", parsed.fileExt.trim());
    }
}

