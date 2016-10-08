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

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Objects;

@NotThreadSafe
public class MemoryAndFileOutput extends DataOutputStream {
    private final PositionedOutputStream stream;

    public MemoryAndFileOutput(String filename, MemoryContext<Integer> memoryContext) throws IOException {
        super(new PositionedOutputStream(filename, memoryContext));
        this.stream = (PositionedOutputStream) out;
    }

    public void setPosition(int position) throws IOException {
        stream.setPosition(position);
    }

    private static class PositionedOutputStream extends OutputStream {
        private final RandomAccessFile file;
        private final MemoryContext<Integer> memoryContext;
        private int position = 0;

        public PositionedOutputStream(String filename, MemoryContext<Integer> memoryContext) throws FileNotFoundException {
            this.file = new RandomAccessFile(filename, "rw");
            this.memoryContext = Objects.requireNonNull(memoryContext);
        }

        @Override
        public void write(int b) throws IOException {
            memoryContext.write(position, b);
            file.write(b);
        }

        public void setPosition(int position) throws IOException {
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
}
