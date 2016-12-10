/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.ssem.assembler;

import emulib.plugins.memory.MemoryContext;
import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Objects;

@NotThreadSafe
public class MemoryAndFileOutput extends SeekableOutputStream {
    private final RandomAccessFile file;
    private final MemoryContext<Byte> memoryContext;
    private int position = 0;

    public MemoryAndFileOutput(String filename, MemoryContext<Byte> memoryContext) throws IOException {
        this.file = new RandomAccessFile(filename, "rw");
        this.memoryContext = Objects.requireNonNull(memoryContext);
    }

    @Override
    public void write(int b) throws IOException {
        memoryContext.write(position, (byte)(b & 0xFF));
        file.write(b);
        position++;
    }

    @Override
    public void seek(int position) throws IOException {
        this.position = position;
        file.seek(position);
    }

    @Override
    public void close() throws IOException {
        try {
            file.close();
        } finally {
            super.close();
        }
    }
}
