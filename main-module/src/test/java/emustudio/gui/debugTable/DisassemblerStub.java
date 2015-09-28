package emustudio.gui.debugTable;

import emulib.plugins.cpu.AbstractDisassembler;
import emulib.plugins.cpu.Decoder;
import emulib.plugins.cpu.DisassembledInstruction;
import emulib.plugins.cpu.InvalidInstructionException;

import static org.easymock.EasyMock.createNiceMock;

public class DisassemblerStub extends AbstractDisassembler {
    private final int[] nextPositions;

    public DisassemblerStub(int memorySize, int... nextPositions) {
        super(createNiceMock(Decoder.class));

        if (memorySize < nextPositions.length) {
            throw new IllegalArgumentException("Memory size < instruction.length");
        }

        this.nextPositions = new int[memorySize];
        for (int i = 0; i < nextPositions.length; i++) {
            this.nextPositions[i] = nextPositions[i];
        }
    }

    public void set(int address, int value) {
        nextPositions[address] = value;
    }

    @Override
    public DisassembledInstruction disassemble(int i) throws InvalidInstructionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getNextInstructionPosition(int position) throws IndexOutOfBoundsException {
        return nextPositions[position];
    }
}
