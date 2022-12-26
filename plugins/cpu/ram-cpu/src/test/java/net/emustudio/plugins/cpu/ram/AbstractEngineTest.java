/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.cpu.ram;

import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import net.emustudio.plugins.memory.ram.api.RAMLabel;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;
import net.emustudio.plugins.memory.ram.api.RAMValue;
import org.junit.Before;

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

    public RAMInstruction instr(RAMInstruction.Opcode opcode, RAMLabel label) {
        RAMInstruction instruction = createNiceMock(RAMInstruction.class);
        expect(instruction.getOpcode()).andReturn(opcode).anyTimes();
        expect(instruction.getDirection()).andReturn(RAMInstruction.Direction.DIRECT).anyTimes();
        expect(instruction.getLabel()).andReturn(Optional.of(label)).anyTimes();
        replay(instruction);
        return instruction;
    }

    public RAMLabel label(int address, String label) {
        return new RAMLabel() {
            @Override
            public int getAddress() {
                return address;
            }

            @Override
            public String getLabel() {
                return label;
            }
        };
    }
}
