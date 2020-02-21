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

import net.sf.emustudio.cpu.testsuite.CpuVerifier;
import net.sf.emustudio.cpu.testsuite.memory.ShortMemoryStub;
import net.sf.emustudio.intel8080.impl.CpuImpl;

import java.util.Objects;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.*;
import static org.junit.Assert.*;

public class CpuVerifierImpl extends CpuVerifier {
    private final CpuImpl cpu;

    public CpuVerifierImpl(CpuImpl cpu, ShortMemoryStub memoryStub) {
        super(memoryStub);
        this.cpu = Objects.requireNonNull(cpu);
    }

    public void checkRegister(int register, int value) {
        value &= 0xFF;
        assertEquals(
            String.format("Expected reg[%02x]=%02x, but was %02x", register, value, cpu.getEngine().regs[register]),
            value, cpu.getEngine().regs[register]
        );
    }

    public void checkRegisterPair(int registerPair, int value) {
        value &= 0xFFFF;

        int realValue;
        switch (registerPair) {
            case 0:
                realValue = cpu.getEngine().regs[REG_B] << 8 | (cpu.getEngine().regs[REG_C]);
                break;
            case 1:
                realValue = cpu.getEngine().regs[REG_D] << 8 | (cpu.getEngine().regs[REG_E]);
                break;
            case 2:
                realValue = cpu.getEngine().regs[REG_H] << 8 | (cpu.getEngine().regs[REG_L]);
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
            int realValue = (cpu.getEngine().regs[REG_A] << 8) | (cpu.getEngine().flags & 0xD7 | 2);
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
        if ((flags & FLAG_S) == FLAG_S) {
            flagsString += "S";
        }
        if ((flags & FLAG_Z) == FLAG_Z) {
            flagsString += "Z";
        }
        if ((flags & FLAG_AC) == FLAG_AC) {
            flagsString += "A";
        }
        if ((flags & FLAG_P) == FLAG_P) {
            flagsString += "P";
        }
        if ((flags & FLAG_C) == FLAG_C) {
            flagsString += "C";
        }
        return flagsString;
    }

    @Override
    public void checkFlags(int mask) {
        assertTrue(
            String.format("Expected flags=%s, but was %s",
                intToFlags(mask), intToFlags(cpu.getEngine().flags)),
            (cpu.getEngine().flags & mask) == mask
        );
    }

    @Override
    public void checkNotFlags(int mask) {
        assertTrue(
            String.format("Expected NOT flags=%s, but was %s",
                intToFlags(mask), intToFlags(cpu.getEngine().flags)),
            (cpu.getEngine().flags & mask) == 0
        );
    }
}
