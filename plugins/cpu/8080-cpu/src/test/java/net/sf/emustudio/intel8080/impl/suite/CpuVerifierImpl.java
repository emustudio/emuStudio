package net.sf.emustudio.intel8080.impl.suite;

import net.sf.emustudio.cpu.testsuite.CpuVerifier;
import net.sf.emustudio.cpu.testsuite.MemoryStub;
import net.sf.emustudio.intel8080.impl.CpuImpl;

import java.util.Objects;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_AC;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_P;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_S;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_Z;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_A;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_B;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_C;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_D;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_E;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.REG_L;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static net.sf.emustudio.intel8080.impl.EmulatorEngine.FLAG_C;

public class CpuVerifierImpl extends CpuVerifier {
    private final CpuImpl cpu;

    public CpuVerifierImpl(CpuImpl cpu, MemoryStub memoryStub) {
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
