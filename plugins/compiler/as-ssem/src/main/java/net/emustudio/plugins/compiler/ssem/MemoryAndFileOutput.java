/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.compiler.ssem;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.io.RandomAccessFile;

@NotThreadSafe
public class MemoryAndFileOutput extends SeekableOutputStream {
    private final RandomAccessFile file;
    private final MemoryContext<Byte> memoryContext;
    private int position = 0;

    public MemoryAndFileOutput(String filename, MemoryContext<Byte> memoryContext) throws IOException {
        this.file = new RandomAccessFile(filename, "rw");
        this.memoryContext = memoryContext;
    }

    @Override
    public void write(int b) throws IOException {
        if (memoryContext != null) {
            memoryContext.write(position, (byte) (b & 0xFF));
        }
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
