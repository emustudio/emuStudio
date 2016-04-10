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
package net.sf.emustudio.zilogZ80.impl.suite;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.MemoryStub;
import net.sf.emustudio.zilogZ80.impl.CpuImpl;
import net.sf.emustudio.zilogZ80.impl.FakeDevice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_A;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_B;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_D;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_E;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_L;

public class CpuRunnerImpl extends CpuRunner<CpuImpl> {
    private final List<FakeDevice> devices;

    public CpuRunnerImpl(CpuImpl cpu, MemoryStub memoryStub, List<FakeDevice> devices) {
        super(cpu, memoryStub);
        this.devices = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(devices)));
    }

    @Override
    public void setRegister(int register, int value) {
        cpu.getEngine().regs[register] = value & 0xFF;
    }

    public void setRegister2(int register, int value) {
        cpu.getEngine().regs2[register] = value & 0xFF;
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
            cpu.getEngine().flags = value & 0xFF;
        } else {
            throw new IllegalArgumentException("Expected value between <0,3> !");
        }
    }

    public void setRegisterPair2(int registerPair, int value) {
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
            default:
                throw new IllegalArgumentException("Expected value between <0,2> !");
        }
        cpu.getEngine().regs2[highRegister] = (value >>> 8) & 0xFF;
        cpu.getEngine().regs2[lowRegister] = value & 0xFF;
    }

    public boolean getIFF(int index) {
        return cpu.getEngine().IFF[index];
    }

    public FakeDevice getDevice(int port) {
        return devices.get(port);
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

    public void setFlags2(int mask) {
        cpu.getEngine().flags2 |= mask;
    }

    public void resetFlags() {
        cpu.getEngine().flags = 0;
    }

    public void resetFlags2() {
        cpu.getEngine().flags2 = 0;
    }

    @Override
    public int getFlags() {
        return cpu.getEngine().flags;
    }

    public void setIX(int ix) {
        cpu.getEngine().IX = ix;
    }

    public void setI(int value) {
        cpu.getEngine().I = value & 0xFF;
    }

    public void setR(int value) {
        cpu.getEngine().R = value & 0xFF;
    }

    public void setIY(int iy) {
        cpu.getEngine().IY = iy;
    }

    public void enableIFF2() {
        cpu.getEngine().IFF[1] = true;
    }

    public void disableIFF1() {
        cpu.getEngine().IFF[0] = false;
    }

    public void setIntMode(byte intMode) {
        cpu.getEngine().intMode = intMode;
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
