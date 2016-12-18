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
package net.sf.emustudio.zilogZ80.impl.suite;

import net.sf.emustudio.cpu.testsuite.CpuVerifier;
import net.sf.emustudio.cpu.testsuite.memory.ShortMemoryStub;
import net.sf.emustudio.zilogZ80.impl.CpuImpl;
import net.sf.emustudio.zilogZ80.impl.FakeDevice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_H;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_PV;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_S;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_Z;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_A;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_B;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_D;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_E;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_L;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CpuVerifierImpl extends CpuVerifier {
    private final CpuImpl cpu;
    private final List<FakeDevice> devices;

    public CpuVerifierImpl(CpuImpl cpu, ShortMemoryStub memoryStub, List<FakeDevice> devices) {
        super(memoryStub);
        this.cpu = Objects.requireNonNull(cpu);
        this.devices = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(devices)));
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

        if (registerPair == 3) {
            realValue = cpu.getEngine().SP;
        } else {
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
            realValue = cpu.getEngine().regs[highRegister] << 8 | (cpu.getEngine().regs[lowRegister]);
        }

        assertEquals(
                String.format("Expected regPair[%02x]=%04x, but was %04x", registerPair, value, realValue),
                value, realValue
        );
    }

    public void checkRegisterPair2(int registerPair, int value) {
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

        value &= 0xFFFF;
        int realValue = cpu.getEngine().regs2[highRegister] << 8 | (cpu.getEngine().regs2[lowRegister]);

        assertEquals(
                String.format("Expected regPair2[%02x]=%04x, but was %04x", registerPair, value, realValue),
                value, realValue
        );
    }


    public void checkIX(int value) {
        value &= 0xFFFF;
        assertEquals(
                String.format("Expected IX=%x, but was %x", value, cpu.getEngine().IX),
                value, cpu.getEngine().IX
        );
    }

    public void checkIY(int value) {
        value &= 0xFFFF;
        assertEquals(
                String.format("Expected IY=%x, but was %x", value, cpu.getEngine().IY),
                value, cpu.getEngine().IY
        );
    }

    public void checkRegisterPairPSW(int registerPair, int value) {
        if (registerPair < 3) {
            checkRegisterPair(registerPair, value);
        } else if (registerPair == 3) {
            int realValue = (cpu.getEngine().regs[REG_A] << 8) | cpu.getEngine().flags;
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

    public void checkInterruptsAreEnabled(int set) {
        assertTrue(cpu.getEngine().IFF[set]);
    }

    public void checkInterruptsAreDisabled(int set) {
        assertFalse(cpu.getEngine().IFF[set]);
    }

    public void checkIntMode(int mode) {
        assertEquals(mode, cpu.getEngine().intMode);
    }

    public String intToFlags(int flags) {
        String flagsString = "";
        if ((flags & FLAG_S) == FLAG_S) {
            flagsString += "S";
        }
        if ((flags & FLAG_Z) == FLAG_Z) {
            flagsString += "Z";
        }
        if ((flags & FLAG_H) == FLAG_H) {
            flagsString += "H";
        }
        if ((flags & FLAG_PV) == FLAG_PV) {
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

    public void checkI(int value) {
        assertEquals(
                String.format("Expected I=%02x, but was %02x", value, cpu.getEngine().I),
                value, cpu.getEngine().I
        );
    }

    public void checkR(int value) {
        assertEquals(
                String.format("Expected R=%02x, but was %02x", value, cpu.getEngine().R),
                value, cpu.getEngine().R
        );
    }

    public void checkAF(int value) {
        int af = (cpu.getEngine().regs[REG_A] << 8) | cpu.getEngine().flags;

        assertEquals(
                String.format("Expected AF=%04x, but was %04x", value, af),
                value, af
        );
    }


    public void checkAF2(int value) {
        int af = (cpu.getEngine().regs2[REG_A] << 8) | cpu.getEngine().flags2;

        assertEquals(
                String.format("Expected AF2=%04x, but was %04x", value, af),
                value, af
        );
    }

    public void checkDeviceValue(int port, int expected) {
        int value = devices.get(port & 0xFF).getValue() & 0xFF;
        assertEquals(
                String.format("Expected device[%02x]=%02x, but was %02x", port, expected, value),
                expected, value
        );
    }
}
