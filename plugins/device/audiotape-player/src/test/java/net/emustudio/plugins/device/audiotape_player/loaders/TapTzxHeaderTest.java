/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player.loaders;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.assertEquals;

public class TapTzxHeaderTest {

    @Test
    public void testConstructor() {
        TapTzxHeader header = new TapTzxHeader(0, "testfile  ", 1024, 100, 200);
        assertEquals(0, header.id);
        assertEquals("testfile  ", header.fileName);
        assertEquals(1024, header.dataLength);
        assertEquals(100, header.parameter1);
        assertEquals(200, header.parameter2);
    }

    @Test
    public void testGetVariable() {
        // variable char is stored in upper byte of parameter1
        int parameter1 = ('B' << 8);
        TapTzxHeader header = new TapTzxHeader(1, "test      ", 100, parameter1, 0);
        assertEquals('B', header.getVariable());
    }

    @Test
    public void testGetVariableForA() {
        int parameter1 = ('A' << 8) | 0xFF;
        TapTzxHeader header = new TapTzxHeader(1, "test      ", 100, parameter1, 0);
        assertEquals('A', header.getVariable());
    }

    @Test
    public void testParse() {
        // Build a buffer: 1 byte id, 10 bytes filename, 2 bytes dataLength, 2 bytes param1, 2 bytes param2
        ByteBuffer buffer = ByteBuffer.allocate(17);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 3); // id = memory block
        buffer.put("HelloWorld".getBytes()); // 10-byte filename
        buffer.putShort((short) 512); // dataLength
        buffer.putShort((short) 32768); // parameter1 (startAddress)
        buffer.putShort((short) 0); // parameter2
        buffer.flip();

        TapTzxHeader header = TapTzxHeader.parse(buffer);
        assertEquals(3, header.id);
        assertEquals("HelloWorld", header.fileName);
        assertEquals(512, header.dataLength);
        assertEquals(32768, header.parameter1);
        assertEquals(0, header.parameter2);
    }

    @Test
    public void testParseProgram() {
        ByteBuffer buffer = ByteBuffer.allocate(17);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 0); // id = program
        buffer.put("MyProgram ".getBytes()); // 10-byte filename (padded)
        buffer.putShort((short) 256); // dataLength
        buffer.putShort((short) 10); // parameter1 (autoStart line)
        buffer.putShort((short) 200); // parameter2 (programLength)
        buffer.flip();

        TapTzxHeader header = TapTzxHeader.parse(buffer);
        assertEquals(0, header.id);
        assertEquals("MyProgram ", header.fileName);
        assertEquals(256, header.dataLength);
        assertEquals(10, header.parameter1);
        assertEquals(200, header.parameter2);
    }

    @Test
    public void testParseFileNameWithSpaces() {
        ByteBuffer buffer = ByteBuffer.allocate(17);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) 0);
        buffer.put("          ".getBytes()); // all spaces
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.flip();

        TapTzxHeader header = TapTzxHeader.parse(buffer);
        assertEquals("          ", header.fileName);
    }
}

