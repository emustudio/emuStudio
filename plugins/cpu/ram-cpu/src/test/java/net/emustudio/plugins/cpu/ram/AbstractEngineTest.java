package net.emustudio.plugins.cpu.ram;

import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;
import net.emustudio.plugins.memory.ram.api.RAMValue;
import org.junit.Before;

import java.util.List;
import java.util.Optional;

import static org.easymock.EasyMock.*;

public abstract class AbstractEngineTest {

    protected RAMMemoryContext memory;
    protected AbstractTapeContext input;
    protected AbstractTapeContext output;
    protected AbstractTapeContext storage;
    protected EmulatorEngine engine;

    @Before
    public void setup() {
        memory = createNiceMock(RAMMemoryContext.class);
        input = createNiceMock(AbstractTapeContext.class);
        output = createNiceMock(AbstractTapeContext.class);
        storage = createNiceMock(AbstractTapeContext.class);
        engine = new EmulatorEngine(input, output, storage, memory);
    }

    public void setProgram(RAMInstruction... program) {
        int i = 0;
        for (RAMInstruction instruction : program) {
            expect(memory.read(i++)).andReturn(instruction).anyTimes();
        }
        replay(memory);
    }

    public RAMValue value(String operand) {
        RAMValue value = createNiceMock(RAMValue.class);
        expect(value.getType()).andReturn(RAMValue.Type.STRING).anyTimes();
        expect(value.getStringValue()).andReturn(operand).anyTimes();
        expect(value.getStringRepresentation()).andReturn(operand).anyTimes();
        replay(value);
        return value;
    }

    public RAMValue value(int operand) {
        RAMValue value = createNiceMock(RAMValue.class);
        expect(value.getType()).andReturn(RAMValue.Type.NUMBER).anyTimes();
        expect(value.getNumberValue()).andReturn(operand).anyTimes();
        expect(value.getStringRepresentation()).andReturn(String.valueOf(operand)).anyTimes();
        replay(value);
        return value;
    }


    public RAMInstruction instr(RAMInstruction.Opcode opcode, RAMInstruction.Direction direction, int operand) {
        RAMInstruction instruction = createNiceMock(RAMInstruction.class);
        expect(instruction.getOpcode()).andReturn(opcode).anyTimes();
        expect(instruction.getDirection()).andReturn(direction).anyTimes();
        expect(instruction.getOperand()).andReturn(Optional.of(value(operand))).anyTimes();
        expect(instruction.getLabel()).andReturn(Optional.empty()).anyTimes();
        replay(instruction);
        return instruction;
    }

    public RAMInstruction instr(RAMInstruction.Opcode opcode, RAMInstruction.Direction direction, String operand) {
        RAMInstruction instruction = createNiceMock(RAMInstruction.class);
        expect(instruction.getOpcode()).andReturn(opcode).anyTimes();
        expect(instruction.getDirection()).andReturn(direction).anyTimes();
        expect(instruction.getOperand()).andReturn(Optional.of(value(operand))).anyTimes();
        expect(instruction.getLabel()).andReturn(Optional.empty()).anyTimes();
        replay(instruction);
        return instruction;
    }

    public RAMInstruction instr(RAMInstruction.Opcode opcode, RAMInstruction.Direction direction) {
        RAMInstruction instruction = createNiceMock(RAMInstruction.class);
        expect(instruction.getOpcode()).andReturn(opcode).anyTimes();
        expect(instruction.getDirection()).andReturn(direction).anyTimes();
        expect(instruction.getOperand()).andReturn(Optional.empty()).anyTimes();
        expect(instruction.getLabel()).andReturn(Optional.empty()).anyTimes();
        replay(instruction);
        return instruction;
    }
}
