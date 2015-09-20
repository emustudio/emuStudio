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
package net.sf.emustudio.zilogZ80.impl.suite.injectors;

import net.sf.emustudio.cpu.testsuite.runners.SingleOperandInjector;
import net.sf.emustudio.zilogZ80.impl.suite.CpuRunnerImpl;

public class Register implements SingleOperandInjector<Byte, CpuRunnerImpl> {
    private final int register;

    public Register(int register) {
        this.register = register;
    }

    @Override
    public void inject(CpuRunnerImpl cpuRunner, Byte value) {
        cpuRunner.setRegister(register, value);
    }

    @Override
    public String toString() {
        return String.format("register[%02x]", register);
    }
}
