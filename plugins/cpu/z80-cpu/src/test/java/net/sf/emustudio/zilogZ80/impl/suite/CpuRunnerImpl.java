package net.sf.emustudio.zilogZ80.impl.suite;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.MemoryStub;
import net.sf.emustudio.zilogZ80.impl.CpuImpl;

import java.util.ArrayList;
import java.util.List;

import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_A;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_B;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_D;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_E;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_H;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.REG_L;

public class CpuRunnerImpl extends CpuRunner<CpuImpl> {

    public CpuRunnerImpl(CpuImpl cpu, MemoryStub memoryStub) {
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
            cpu.getEngine().flags = (short)(value & 0xD7 | 2);
        } else {
            throw new IllegalArgumentException("Expected value between <0,3> !");
        }
    }

    public Byte getRegister(int register) {
        return new Byte((byte)cpu.getEngine().regs[register]);
    }

    @Override
    public int getPC() {
        return cpu.getEngine().PC;
    }

    public void setSP(int SP) {
        cpu.getEngine().SP = SP;
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
    public void resetFlags(int mask) {
        cpu.getEngine().flags &= ~mask;
    }

    @Override
    public int getFlags() {
        return cpu.getEngine().flags;
    }

    public void setIX(int ix) {
        cpu.getEngine().IX = ix;
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

    @Override
    public List<Integer> getRegisters() {
        List<Integer> registers = new ArrayList<>();
        for (int reg : cpu.getEngine().regs) {
            registers.add(reg);
        }
        return registers;
    }
}
