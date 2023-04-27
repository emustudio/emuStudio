/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.cassette_player.loaders;

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
