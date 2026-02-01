/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player.loaders;

import net.jcip.annotations.Immutable;

import java.nio.ByteBuffer;

@Immutable
public class TapTzxHeader {
    final int id;
    final String fileName;
    final int dataLength;
    final int parameter1;
    final int parameter2;

    public TapTzxHeader(int id, String fileName, int dataLength, int parameter1, int parameter2) {
        this.id = id;
        this.fileName = fileName;
        this.dataLength = dataLength;
        this.parameter1 = parameter1;
        this.parameter2 = parameter2;
    }

    public char getVariable() {
        return (char) ((parameter1 >>> 8) & 0xFF);
    }

    public static TapTzxHeader parse(ByteBuffer buffer) {
        int headerFlag = buffer.get() & 0xFF;
        byte[] fileName = new byte[10];
        buffer.get(fileName); // filename
        int dataLength = buffer.getShort() & 0xFFFF; // length

        int parameter1 = buffer.getShort() & 0xFFFF;
        int parameter2 = buffer.getShort() & 0xFFFF;

        return new TapTzxHeader(headerFlag, new String(fileName), dataLength, parameter1, parameter2);
    }
}
