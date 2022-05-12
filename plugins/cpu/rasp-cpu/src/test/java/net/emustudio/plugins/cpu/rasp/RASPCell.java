package net.emustudio.plugins.cpu.rasp;

import net.emustudio.plugins.memory.rasp.api.RASPMemoryCell;

public class RASPCell implements RASPMemoryCell {
    private final boolean isInstruction;
    private final int address;
    private final int value;

    private RASPCell(boolean isInstruction, int address, int value) {
        this.isInstruction = isInstruction;
        this.address = address;
        this.value = value;
    }

    @Override
    public boolean isInstruction() {
        return isInstruction;
    }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public int getValue() {
        return value;
    }

    public static RASPCell instruction(int address, int opcode) {
        return new RASPCell(true, address, opcode);
    }

    public static RASPCell operand(int address, int value) {
        return new RASPCell(false, address, value);
    }
}
