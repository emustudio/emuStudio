/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamLabel;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import net.emustudio.plugins.memory.ram.api.RamValue;
import org.junit.Before;

import java.util.Optional;

import static org.easymock.EasyMock.*;

public abstract class AbstractEngineTest {

    protected RamMemoryContext memory;
    protected AbstractTapeContext input;
    protected AbstractTapeContext output;
    protected AbstractTapeContext storage;
    protected EmulatorEngine engine;

    @Before
    public void setup() {
        memory = createNiceMock(RamMemoryContext.class);
        input = createNiceMock(AbstractTapeContext.class);
        output = createNiceMock(AbstractTapeContext.class);
        storage = createNiceMock(AbstractTapeContext.class);
        engine = new EmulatorEngine(input, output, storage, memory);
    }

    public void setProgram(RamInstruction... program) {
        int i = 0;
        for (RamInstruction instruction : program) {
            expect(memory.read(i++)).andReturn(instruction).anyTimes();
        }
        replay(memory);
    }

    public RamValue value(String operand) {
        RamValue value = createNiceMock(RamValue.class);
        expect(value.getType()).andReturn(RamValue.Type.STRING).anyTimes();
        expect(value.getStringValue()).andReturn(operand).anyTimes();
        expect(value.getStringRepresentation()).andReturn(operand).anyTimes();
        replay(value);
        return value;
    }

    public RamValue value(int operand) {
        RamValue value = createNiceMock(RamValue.class);
        expect(value.getType()).andReturn(RamValue.Type.NUMBER).anyTimes();
        expect(value.getNumberValue()).andReturn(operand).anyTimes();
        expect(value.getStringRepresentation()).andReturn(String.valueOf(operand)).anyTimes();
        replay(value);
        return value;
    }


    public RamInstruction instr(RamInstruction.Opcode opcode, RamInstruction.Direction direction, int operand) {
        RamInstruction instruction = createNiceMock(RamInstruction.class);
        expect(instruction.getOpcode()).andReturn(opcode).anyTimes();
        expect(instruction.getDirection()).andReturn(direction).anyTimes();
        expect(instruction.getOperand()).andReturn(Optional.of(value(operand))).anyTimes();
        expect(instruction.getLabel()).andReturn(Optional.empty()).anyTimes();
        replay(instruction);
        return instruction;
    }

    public RamInstruction instr(RamInstruction.Opcode opcode, RamInstruction.Direction direction, String operand) {
        RamInstruction instruction = createNiceMock(RamInstruction.class);
        expect(instruction.getOpcode()).andReturn(opcode).anyTimes();
        expect(instruction.getDirection()).andReturn(direction).anyTimes();
        expect(instruction.getOperand()).andReturn(Optional.of(value(operand))).anyTimes();
        expect(instruction.getLabel()).andReturn(Optional.empty()).anyTimes();
        replay(instruction);
        return instruction;
    }

    public RamInstruction instr(RamInstruction.Opcode opcode, RamInstruction.Direction direction) {
        RamInstruction instruction = createNiceMock(RamInstruction.class);
        expect(instruction.getOpcode()).andReturn(opcode).anyTimes();
        expect(instruction.getDirection()).andReturn(direction).anyTimes();
        expect(instruction.getOperand()).andReturn(Optional.empty()).anyTimes();
        expect(instruction.getLabel()).andReturn(Optional.empty()).anyTimes();
        replay(instruction);
        return instruction;
    }

    public RamInstruction instr(RamInstruction.Opcode opcode, RamLabel label) {
        RamInstruction instruction = createNiceMock(RamInstruction.class);
        expect(instruction.getOpcode()).andReturn(opcode).anyTimes();
        expect(instruction.getDirection()).andReturn(RamInstruction.Direction.DIRECT).anyTimes();
        expect(instruction.getLabel()).andReturn(Optional.of(label)).anyTimes();
        replay(instruction);
        return instruction;
    }

    public RamLabel label(int address, String label) {
        return new RamLabel() {
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
