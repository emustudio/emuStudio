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
package net.sf.emustudio.zilogZ80.impl.suite.injectors;

import net.sf.emustudio.zilogZ80.impl.suite.CpuRunnerImpl;

import java.util.function.BiConsumer;

public class RegisterPair2 implements BiConsumer<CpuRunnerImpl, Integer> {
    private final int registerPair;

    public RegisterPair2(int registerPair) {
        this.registerPair = registerPair;
    }

    @Override
    public void accept(CpuRunnerImpl cpuRunner, Integer value) {
        cpuRunner.setRegisterPair2(registerPair, value & 0xFFFF);
    }

    @Override
    public String toString() {
        return String.format("registerPair2[%04x]", registerPair);
    }

}
