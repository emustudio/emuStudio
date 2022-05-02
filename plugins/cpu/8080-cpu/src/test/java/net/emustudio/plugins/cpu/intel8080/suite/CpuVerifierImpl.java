/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.cpu.intel8080.suite;

import net.emustudio.cpu.testsuite.CpuVerifier;
import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.plugins.cpu.intel8080.CpuImpl;
import net.emustudio.plugins.cpu.intel8080.EmulatorEngine;

import java.util.Objects;

import static org.junit.Assert.*;

public class CpuVerifierImpl extends CpuVerifier {
    private final CpuImpl cpu;

    public CpuVerifierImpl(CpuImpl cpu, ByteMemoryStub memoryStub) {
        super(memoryStub);
        this.cpu = Objects.requireNonNull(cpu);
    }

    public void checkRegister(int register, int expected) {
        expected &= 0xFF;
        int actual = cpu.getEngine().regs[register] & 0xFF;
        assertEquals(
            String.format("Expected reg[%02x]=%02x, but was %02x", register, expected, actual),
            expected, actual
        );
    }

    public void checkRegisterPair(int registerPair, int value) {
        value &= 0xFFFF;

        int realValue;
        switch (registerPair) {
            case 0:
                realValue = cpu.getEngine().regs[EmulatorEngine.REG_B] << 8 | (cpu.getEngine().regs[EmulatorEngine.REG_C]);
                break;
            case 1:
                realValue = cpu.getEngine().regs[EmulatorEngine.REG_D] << 8 | (cpu.getEngine().regs[EmulatorEngine.REG_E]);
                break;
            case 2:
                realValue = cpu.getEngine().regs[EmulatorEngine.REG_H] << 8 | (cpu.getEngine().regs[EmulatorEngine.REG_L]);
                break;
            case 3:
                realValue = cpu.getEngine().SP;
                break;
            default:
                throw new IllegalArgumentException("Expected value between <0,3> !");
        }

        assertEquals(
            String.format("Expected regPair[%02x]=%04x, but was %04x", registerPair, value, realValue),
            value, realValue
        );
    }

    public void checkRegisterPairPSW(int registerPair, int value) {
        if (registerPair < 3) {
            checkRegisterPair(registerPair, value);
        } else if (registerPair == 3) {
            int realValue = (cpu.getEngine().regs[EmulatorEngine.REG_A] << 8) | (cpu.getEngine().flags & 0xD7 | 2);
            assertEquals(
                String.format("Expected regPair[%02x]=%04x, but was %04x", registerPair, value, realValue),
                value, realValue
            );
        } else {
            throw new IllegalArgumentException("Expected value between <0,3> !");
        }
    }

    public void checkPC(int PC) {
        assertEquals(
            String.format("Expected PC=%04x, but was %04x", PC, cpu.getEngine().PC),
            PC, cpu.getEngine().PC
        );
    }

    public void checkInterruptsAreEnabled() {
        assertTrue(cpu.getEngine().INTE);
    }

    public void checkInterruptsAreDisabled() {
        assertFalse(cpu.getEngine().INTE);
    }

    public String intToFlags(int flags) {
        String flagsString = "";
        if ((flags & EmulatorEngine.FLAG_S) == EmulatorEngine.FLAG_S) {
            flagsString += "S";
        }
        if ((flags & EmulatorEngine.FLAG_Z) == EmulatorEngine.FLAG_Z) {
            flagsString += "Z";
        }
        if ((flags & EmulatorEngine.FLAG_AC) == EmulatorEngine.FLAG_AC) {
            flagsString += "A";
        }
        if ((flags & EmulatorEngine.FLAG_P) == EmulatorEngine.FLAG_P) {
            flagsString += "P";
        }
        if ((flags & EmulatorEngine.FLAG_C) == EmulatorEngine.FLAG_C) {
            flagsString += "C";
        }
        return flagsString;
    }

    @Override
    public void checkFlags(int mask) {
        assertEquals(String.format("Expected flags=%s, but was %s",
            intToFlags(mask), intToFlags(cpu.getEngine().flags)), (cpu.getEngine().flags & mask), mask);
    }

    @Override
    public void checkNotFlags(int mask) {
        assertEquals(String.format("Expected NOT flags=%s, but was %s",
            intToFlags(mask), intToFlags(cpu.getEngine().flags)), 0, (cpu.getEngine().flags & mask));
    }
}
