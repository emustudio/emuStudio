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
package net.emustudio.plugins.cpu.zilogZ80.suite;

import net.emustudio.cpu.testsuite.CpuVerifier;
import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.plugins.cpu.zilogZ80.CpuImpl;
import net.emustudio.plugins.cpu.zilogZ80.FakeByteDevice;

import java.util.List;
import java.util.Objects;

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;
import static org.junit.Assert.*;

public class CpuVerifierImpl extends CpuVerifier {
    private final CpuImpl cpu;
    private final List<FakeByteDevice> devices;

    public CpuVerifierImpl(CpuImpl cpu, ByteMemoryStub memoryStub, List<FakeByteDevice> devices) {
        super(memoryStub);
        this.cpu = Objects.requireNonNull(cpu);
        this.devices = List.copyOf(Objects.requireNonNull(devices));
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

    public static String intToFlags(int flags) {
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
        if ((flags & FLAG_N) == FLAG_N) {
            flagsString += "N";
        }
        if ((flags & FLAG_C) == FLAG_C) {
            flagsString += "C";
        }
        return flagsString;
    }

    @Override
    public void checkFlags(int mask) {
        assertEquals(String.format("Expected flags=%s, but was %s",
            intToFlags(mask), intToFlags(cpu.getEngine().flags)), mask, (cpu.getEngine().flags & mask));
    }

    @Override
    public void checkNotFlags(int mask) {
        assertEquals(String.format("Expected NOT flags=%s, but was %s",
            intToFlags(mask), intToFlags(cpu.getEngine().flags)), 0, (cpu.getEngine().flags & mask));
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
