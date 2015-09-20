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
import net.sf.emustudio.cpu.testsuite.runners.SingleOperandInjector;

public class MemoryAddress<TCpuRunnerType extends CpuRunner>
        implements SingleOperandInjector<Integer, TCpuRunnerType> {
    private final int value;
    private final boolean word;

    public MemoryAddress(Byte value) {
        this.value = value & 0xFF;
        word = false;
    }

    public MemoryAddress(Integer value) {
        this.value = value & 0xFFFF;
        word = true;
    }

    @Override
    public void inject(CpuRunner cpuRunner, Integer address) {
        cpuRunner.setByte(address, value & 0xFF);
        if (word) {
            cpuRunner.setByte(address + 1, (value >>> 8) & 0xFF);
        }
    }

    @Override
    public String toString() {
        return String.format("memory[address] = %04x (word=%s)", value, word);
    }

}
