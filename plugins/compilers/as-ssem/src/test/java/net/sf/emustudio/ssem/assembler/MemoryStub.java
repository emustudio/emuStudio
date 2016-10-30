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

import emulib.plugins.memory.AbstractMemoryContext;

public class MemoryStub extends AbstractMemoryContext<Byte> {
    private final byte[] memory = new byte[1000];

    @Override
    public Byte read(int memoryPosition) {
        return memory[memoryPosition];
    }

    @Override
    public Byte[] readWord(int memoryPosition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(int memoryPosition, Byte value) {
        memory[memoryPosition] = value;
    }

    @Override
    public void writeWord(int memoryPosition, Byte[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<?> getDataType() {
        return Byte.class;
    }

    @Override
    public void clear() {

    }

    @Override
    public int getSize() {
        return memory.length;
    }
}
