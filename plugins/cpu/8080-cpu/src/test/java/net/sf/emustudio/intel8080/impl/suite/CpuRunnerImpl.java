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
package net.sf.emustudio.intel8080.impl.suite;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.memory.ShortMemoryStub;
import net.sf.emustudio.intel8080.impl.CpuImpl;

import java.util.ArrayList;
import java.util.List;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.*;

public class CpuRunnerImpl extends CpuRunner<CpuImpl> {

    public CpuRunnerImpl(CpuImpl cpu, ShortMemoryStub memoryStub) {
        super(cpu, memoryStub);
    }

    @Override
    public void setRegister(int register, int value) {
        cpu.getEngine().regs[register] = value & 0xFF;
    }

    public void setRegisterPair(int registerPair, int value) {
        int highRegister;
        int lowRegister;
        switch (registerPair) {
            case 0:
                highRegister = REG_B;
                lowRegister = REG_C;
                break;
            case 1:
                highRegister = REG_D;
                lowRegister = REG_E;
                break;
            case 2:
                highRegister = REG_H;
                lowRegister = REG_L;
                break;
            case 3:
                cpu.getEngine().SP = value & 0xFFFF;
                return;
            default:
                throw new IllegalArgumentException("Expected value between <0,3> !");
        }
        cpu.getEngine().regs[highRegister] = (value >>> 8) & 0xFF;
        cpu.getEngine().regs[lowRegister] = value & 0xFF;
    }

    public void setRegisterPairPSW(int registerPair, int value) {
        if (registerPair < 3) {
            setRegisterPair(registerPair, value);
        } else if (registerPair == 3) {
            cpu.getEngine().regs[REG_A] = (value >>> 8) & 0xFF;
            cpu.getEngine().flags = (short) (value & 0xD7 | 2);
        } else {
            throw new IllegalArgumentException("Expected value between <0,3> !");
        }
    }

    @Override
    public int getPC() {
        return cpu.getEngine().PC;
    }

    @Override
    public int getSP() {
        return cpu.getEngine().SP;
    }

    @Override
    public void setFlags(int mask) {
        cpu.getEngine().flags |= mask;
    }

    @Override
    public int getFlags() {
        return cpu.getEngine().flags;
    }

    @Override
    public List<Integer> getRegisters() {
        List<Integer> registers = new ArrayList<>();
        for (int reg : cpu.getEngine().regs) {
            registers.add(reg);
        }
        return registers;
    }
}
