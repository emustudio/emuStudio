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
 * Program memory expander.
 *
 * Used as an injector for TestRunner.
 *
 * Ensures that memory has at least specified size. The size is injected from TestRunner.
 *
 */
public class MemoryExpand<T extends CpuRunner> implements BiConsumer<T, Integer> {

    @Override
    public void accept(T cpuRunner, Integer address) {
        cpuRunner.ensureProgramSize(address + 4);
    }

    @Override
    public String toString() {
        return "memoryExpander";
    }
}
