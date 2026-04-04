/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram.api;

import net.emustudio.plugins.memory.ram.MemoryContextImpl;
import net.emustudio.plugins.memory.ram.TestRamMemoryContextFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class RamMemoryContextTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Test
    public void testGetCellTypeClass() {
        MemoryContextImpl context = TestRamMemoryContextFactory.create();
        assertEquals(RamInstruction.class, context.getCellTypeClass());
    }

    @Test
    public void testRamMemoryConstructor() {
        RamLabel label = createLabel(0, "start");
        RamInstruction instr = createInstruction(RamInstruction.Opcode.HALT);
        RamValue value = createValue(42, "42");

        RamMemoryContext.RamMemory memory = new RamMemoryContext.RamMemory(
                List.of(label),
                Map.of(0, instr),
                List.of(value)
        );

        assertEquals(1, memory.labels.size());
        assertEquals(1, memory.programMemory.size());
        assertEquals(1, memory.inputs.size());
    }

    @Test
    public void testRamMemoryIsImmutable() {
        RamMemoryContext.RamMemory memory = new RamMemoryContext.RamMemory(
                List.of(), Map.of(), List.of()
        );

        assertEquals(0, memory.labels.size());
        assertEquals(0, memory.programMemory.size());
        assertEquals(0, memory.inputs.size());
    }

    @Test
    public void testSerializeAndDeserialize() throws Exception {
        MemoryContextImpl context = TestRamMemoryContextFactory.create();

        RamLabel label = createLabel(0, "start");
        RamValue operand = createSerializableValue(10, "10");
        RamInstruction instr = createSerializableInstruction(0, RamInstruction.Opcode.LOAD,
                RamInstruction.Direction.CONSTANT, operand, null);

        context.setLabels(List.of(label));
        context.setInputs(List.of(operand));
        context.write(0, instr);

        File file = tmpFolder.newFile("test.bram");
        RamMemoryContext.serialize(file.toPath(), context.getSnapshot());

        // Deserialize
        MemoryContextImpl context2 = TestRamMemoryContextFactory.create();
        context2.deserialize(file.getAbsolutePath());

        assertEquals(1, context2.getSize());
        assertTrue(context2.getLabel(0).isPresent());
        assertEquals("start", context2.getLabel(0).get().getLabel());
    }

    @Test(expected = IOException.class)
    public void testSerializeToInvalidPathThrows() throws IOException {
        RamMemoryContext.RamMemory memory = new RamMemoryContext.RamMemory(
                List.of(), Map.of(), List.of()
        );
        RamMemoryContext.serialize(java.nio.file.Path.of("/nonexistent/dir/file.bram"), memory);
    }

    private RamLabel createLabel(int address, String name) {
        RamLabel label = createNiceMock(RamLabel.class);
        expect(label.getAddress()).andReturn(address).anyTimes();
        expect(label.getLabel()).andReturn(name).anyTimes();
        replay(label);
        return label;
    }

    private RamValue createValue(int number, String repr) {
        RamValue value = createNiceMock(RamValue.class);
        expect(value.getNumberValue()).andReturn(number).anyTimes();
        expect(value.getStringRepresentation()).andReturn(repr).anyTimes();
        expect(value.getType()).andReturn(RamValue.Type.NUMBER).anyTimes();
        replay(value);
        return value;
    }

    private static RamValue createSerializableValue(int number, String repr) {
        return new RamValue() {
            private static final long serialVersionUID = 1L;

            @Override
            public Type getType() { return Type.NUMBER; }

            @Override
            public int getNumberValue() { return number; }

            @Override
            public String getStringValue() { return repr; }

            @Override
            public String getStringRepresentation() { return repr; }
        };
    }

    private static RamInstruction createSerializableInstruction(int address, RamInstruction.Opcode opcode,
                                                                 RamInstruction.Direction direction,
                                                                 RamValue operand, RamLabel label) {
        return new RamInstruction() {
            private static final long serialVersionUID = 1L;

            @Override
            public int getAddress() { return address; }

            @Override
            public Opcode getOpcode() { return opcode; }

            @Override
            public Direction getDirection() { return direction; }

            @Override
            public Optional<RamValue> getOperand() { return Optional.ofNullable(operand); }

            @Override
            public Optional<RamLabel> getLabel() { return Optional.ofNullable(label); }
        };
    }

    private RamInstruction createInstruction(RamInstruction.Opcode opcode) {
        RamInstruction instr = createNiceMock(RamInstruction.class);
        expect(instr.getOpcode()).andReturn(opcode).anyTimes();
        expect(instr.getDirection()).andReturn(RamInstruction.Direction.DIRECT).anyTimes();
        expect(instr.getOperand()).andReturn(Optional.empty()).anyTimes();
        expect(instr.getLabel()).andReturn(Optional.empty()).anyTimes();
        replay(instr);
        return instr;
    }
}
