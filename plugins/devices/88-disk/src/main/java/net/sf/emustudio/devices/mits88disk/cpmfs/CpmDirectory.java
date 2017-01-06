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

import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@NotThreadSafe
public class CpmDirectory {
    public final static int DIRECTORY_ENTRY_SIZE = 32;
    public final static int DIRECTORY_TRACK = 6;

    private final List<CpmFile> files = new ArrayList<>();

    public CpmDirectory(List<CpmFile> files) {
        this.files.addAll(files);
    }

    public List<CpmFile> getAllFiles() {
        return Collections.unmodifiableList(files);
    }

    public List<CpmFile> filterValidFiles() {
        return files.stream().filter(file -> file.status < 32).collect(Collectors.toList());
    }

    public String findDiscLabel() {
        for (CpmFile file : files) {
            if (file.status == 32) {
                return file.fileName + file.fileExt;
            }
        }
        return "";
    }

    private static List<ByteBuffer> getEntries(List<ByteBuffer> directorySectors) {
        List<ByteBuffer> entries = new ArrayList<>();

        for (ByteBuffer sector : directorySectors) {
            sector.position(3);
            int numberOfEntries = sector.remaining() / DIRECTORY_ENTRY_SIZE;

            for (int i = 0; i < numberOfEntries; i++) {
                byte[] entry = new byte[DIRECTORY_ENTRY_SIZE];
                sector.get(entry);

                entries.add(ByteBuffer.wrap(entry).asReadOnlyBuffer());
            }
        }
        return entries;
    }

    private static List<CpmFile> getFilesFromEntries(List<ByteBuffer> entries) {
        return entries.stream().map(CpmFile::fromEntry).collect(Collectors.toList());
    }

    public static CpmDirectory fromDisc(RawDisc disc) throws IOException {
        disc.reset(DIRECTORY_TRACK);
        List<ByteBuffer> directorySectors = disc.readBlock();
        List<ByteBuffer> entries = getEntries(directorySectors);
        List<CpmFile> files = getFilesFromEntries(entries);

        return new CpmDirectory(files);
    }

}
