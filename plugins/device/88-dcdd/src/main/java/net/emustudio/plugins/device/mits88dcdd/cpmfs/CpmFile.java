/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.device.mits88dcdd.cpmfs;

import net.jcip.annotations.Immutable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFileSystem.ENTRY_SIZE;

@Immutable
public class CpmFile {
    final String fileName;
    final String fileExt;
    final int status;
    final int extentNumber;
    final byte bc; // the number of bytes in the last used record
    final byte rc; // the number of 128 byte records of the last used logical extent.
    final List<Byte> blockPointers; // allocation

    CpmFile(String fileName, String fileExt, int status, int extentNumber, byte bc, byte rc, List<Byte> blockPointers) {
        this.status = status;
        this.extentNumber = extentNumber;
        this.bc = bc;
        this.rc = rc;
        this.fileName = Objects.requireNonNull(fileName);
        this.fileExt = Objects.requireNonNull(fileExt);
        this.blockPointers = Collections.unmodifiableList(Objects.requireNonNull(blockPointers));
    }

    @Override
    public String toString() {
        String result = fileName.trim();

        String ext = fileExt.trim();
        if (!ext.equals("")) {
            result += "." + ext;
        }
        return result;
    }

    static CpmFile fromEntry(ByteBuffer entry) {
        int fileStatus = entry.get() & 0xFF;

        byte[] fileNameBytes = new byte[11];
        entry.get(fileNameBytes);
        String fileName = new String(fileNameBytes);

        int extent = entry.get() & 0x1F; // only bits 0-4 are valid
        byte bc = entry.get();

        // Bit 5-7 of Xl are 0, bit 0-4 store the lower bits of the extent number.
        // Bit 6 and 7 of Xh are 0, bit 0-5 store the higher bits of the extent number.
        // Entry number = ((32*S2)+EX) / (exm+1) where exm is the extent mask value from the Disc Parameter Block
        extent = ((entry.get() & 0x3F) << 5) | extent;
        byte rc = entry.get();

        List<Byte> blockPointers = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            byte bp = entry.get();
            blockPointers.add(bp);
        }

        return new CpmFile(fileName.substring(0, 8), fileName.substring(8, 11), fileStatus, extent, bc, rc, blockPointers);
    }

    ByteBuffer toEntry() {
        ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
        entry.put((byte) status);

        byte[] fileNameBytes = new byte[11];
        int i;
        for (i = 0; i < fileName.length(); i++) {
            fileNameBytes[i] = (byte)(fileName.charAt(i) & 0xFF);
        }
        for (; i < 8; i++) {
            fileNameBytes[i] = 0x20; // space
        }
        for (; i < fileExt.length(); i++) {
            fileNameBytes[i] = (byte)(fileExt.charAt(i) & 0xFF);
        }
        for (; i < 11; i++) {
            fileNameBytes[i] = 0x20; // space
        }
        entry.put(fileNameBytes);

        //Bit 5-7 of Xl are 0, bit 0-4 store the lower bits of the extent number.
        entry.put((byte) (extentNumber & 0x1F));
        entry.put(bc);
        // Bit 6 and 7 of Xh are 0, bit 0-5 store the higher bits of the extent number.
        entry.put((byte) ((extentNumber >>> 5) & 0x3F));
        entry.put(rc);

        for (byte b : blockPointers) {
            entry.put(b);
        }
        return entry.flip();
    }
}
