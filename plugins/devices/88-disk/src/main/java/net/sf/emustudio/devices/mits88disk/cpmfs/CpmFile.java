/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.devices.mits88disk.cpmfs;

import net.jcip.annotations.Immutable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Immutable
public class CpmFile {
    final String fileName;
    final String fileExt;
    final int status;
    final int extentNumber;
    final byte bc; // the number of bytes in the last used record
    final byte rc; // the number of 128 byte records of the last used logical extent.
    final List<Byte> blockPointers;

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

        int extent = entry.get() & 0xFF;
        byte bc = entry.get();
        extent = ((entry.get() & 0xFF) << 8) | extent;
        byte rc = entry.get();

        List<Byte> blockPointers = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            byte bp = entry.get();
            blockPointers.add(bp);
        }

        return new CpmFile(fileName.substring(0, 8), fileName.substring(8, 11), fileStatus, extent, bc, rc, blockPointers);
    }

}
