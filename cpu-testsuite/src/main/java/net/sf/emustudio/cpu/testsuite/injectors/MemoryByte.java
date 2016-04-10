/*
 * Copyright (C) 2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;

import java.util.function.BiConsumer;

/**
 * Injector of a byte value at specified memory address.
 *
 * Given memory address, test runner will inject a 8-bit value there.
 * Higher than 8-bit value will be truncated.
 */
public class MemoryByte<T extends CpuRunner, OperandType extends Number> implements BiConsumer<T, OperandType> {
    private final int address;

    /**
     * Creates new memory byte injector
     *
     * @param address memory address where the byte will be injected
     */
    public MemoryByte(int address) {
        if (address <= 0) {
            throw new IllegalArgumentException("Address can be only > 0! (was " + address + ")");
        }

        this.address = address;
    }

    @Override
    public void accept(T cpuRunner, OperandType value) {
        cpuRunner.setByte(address, value.byteValue());
    }

    @Override
    public String toString() {
        return String.format("memoryByte[%04x]", address);
    }

}
