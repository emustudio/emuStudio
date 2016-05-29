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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

@NotThreadSafe
public class MemoryAndFileOutputStream extends OutputStream {
    private final FileOutputStream fileOutputStream;
    private final MemoryContext<Short> memoryContext;
    int position = 0;

    public MemoryAndFileOutputStream(String filename, MemoryContext<Short> memoryContext) throws FileNotFoundException {
        this.fileOutputStream = new FileOutputStream(filename);
        this.memoryContext = Objects.requireNonNull(memoryContext);
    }

    @Override
    public void write(int b) throws IOException {
        fileOutputStream.write(b);
        memoryContext.write(position++, (short)b);
    }

    @Override
    public void close() throws IOException {
        fileOutputStream.close();
    }
}
