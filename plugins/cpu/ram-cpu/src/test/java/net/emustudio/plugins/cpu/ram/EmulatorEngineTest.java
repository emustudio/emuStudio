/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import net.emustudio.plugins.memory.ram.api.RamValue;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class EmulatorEngineTest extends AbstractEngineTest {

    @Test
    public void testREAD_DIRECT() {
        setProgram(instr(RamInstruction.Opcode.READ, RamInstruction.Direction.DIRECT, 5));

        TapeSymbol symbol = new TapeSymbol("hello");
        expect(input.readData()).andReturn(symbol).once();
        storage.setSymbolAt(eq(5), eq(symbol));
        expectLastCall().once();
        replay(input, storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(input, storage);
    }

    @Test
    public void testREAD_INDIRECT() {
        setProgram(instr(RamInstruction.Opcode.READ, RamInstruction.Direction.INDIRECT, 3));

        TapeSymbol symbol = new TapeSymbol("hello");
        expect(input.readData()).andReturn(symbol).once();
        expect(storage.getSymbolAt(3)).andReturn(Optional.of(new TapeSymbol(5))).once();
        storage.setSymbolAt(eq(5), eq(symbol));
        expectLastCall().once();
        replay(input, storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(input, storage);
    }

    @Test
    public void testWRITE_CONSTANT() {
        setProgram(instr(RamInstruction.Opcode.WRITE, RamInstruction.Direction.CONSTANT, "yoohoo"));
        output.writeData(eq(new TapeSymbol("yoohoo")));
        expectLastCall().once();
        replay(output);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(output);
    }

    @Test
    public void testWRITE_DIRECT() {
        setProgram(instr(RamInstruction.Opcode.WRITE, RamInstruction.Direction.DIRECT, 3));

        expect(storage.getSymbolAt(3)).andReturn(Optional.of(new TapeSymbol("yoohoo"))).once();
        output.writeData(eq(new TapeSymbol("yoohoo")));
        expectLastCall().once();
        replay(storage, output);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage, output);
    }

    @Test
    public void testWRITE_INDIRECT() {
        setProgram(instr(RamInstruction.Opcode.WRITE, RamInstruction.Direction.INDIRECT, 3));

        expect(storage.getSymbolAt(3)).andReturn(Optional.of(new TapeSymbol(5))).once();
        expect(storage.getSymbolAt(5)).andReturn(Optional.of(new TapeSymbol("yoohoo"))).once();
        output.writeData(eq(new TapeSymbol("yoohoo")));
        expectLastCall().once();
        replay(storage, output);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage, output);
    }

    @Test
    public void testLOAD_CONSTANT() {
        setProgram(instr(RamInstruction.Opcode.LOAD, RamInstruction.Direction.CONSTANT, "yoohoo"));
        storage.setSymbolAt(eq(0), eq(new TapeSymbol("yoohoo")));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testLOAD_DIRECT() {
        setProgram(instr(RamInstruction.Opcode.LOAD, RamInstruction.Direction.DIRECT, 3));
        expect(storage.getSymbolAt(3)).andReturn(Optional.of(new TapeSymbol("yoohoo"))).once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol("yoohoo")));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testLOAD_INDIRECT() {
        setProgram(instr(RamInstruction.Opcode.LOAD, RamInstruction.Direction.INDIRECT, 5));
        expect(storage.getSymbolAt(5)).andReturn(Optional.of(new TapeSymbol(3))).once();
        expect(storage.getSymbolAt(3)).andReturn(Optional.of(new TapeSymbol("yoohoo"))).once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol("yoohoo")));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testSTORE_DIRECT() {
        setProgram(instr(RamInstruction.Opcode.STORE, RamInstruction.Direction.DIRECT, 5));

        TapeSymbol symbol = new TapeSymbol("yoohoo");
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(symbol)).once();
        storage.setSymbolAt(eq(5), eq(symbol));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testSTORE_INDIRECT() {
        setProgram(instr(RamInstruction.Opcode.STORE, RamInstruction.Direction.INDIRECT, 3));

        TapeSymbol symbol = new TapeSymbol("yoohoo");
        expect(storage.getSymbolAt(3)).andReturn(Optional.of(new TapeSymbol(5))).once();
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(symbol)).once();
        storage.setSymbolAt(eq(5), eq(symbol));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testArith_CONSTANT() {
        setProgram(
                instr(RamInstruction.Opcode.ADD, RamInstruction.Direction.CONSTANT, 5),
                instr(RamInstruction.Opcode.SUB, RamInstruction.Direction.CONSTANT, -1),
                instr(RamInstruction.Opcode.MUL, RamInstruction.Direction.CONSTANT, 2),
                instr(RamInstruction.Opcode.DIV, RamInstruction.Direction.CONSTANT, 3)
        );
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(-3))).once();
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(2))).once();
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(3))).once();
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(6))).once();

        storage.setSymbolAt(eq(0), eq(new TapeSymbol(2)));
        expectLastCall().once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol(3)));
        expectLastCall().once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol(6)));
        expectLastCall().once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol(2)));
        expectLastCall().once();

        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(4, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testADD_DIRECT() {
        setProgram(
                instr(RamInstruction.Opcode.ADD, RamInstruction.Direction.DIRECT, 3),
                instr(RamInstruction.Opcode.SUB, RamInstruction.Direction.DIRECT, 4),
                instr(RamInstruction.Opcode.MUL, RamInstruction.Direction.DIRECT, 5),
                instr(RamInstruction.Opcode.DIV, RamInstruction.Direction.DIRECT, 6)
        );
        expect(storage.getSymbolAt(3)).andReturn(Optional.of(new TapeSymbol(5))).once();
        expect(storage.getSymbolAt(4)).andReturn(Optional.of(new TapeSymbol(-1))).once();
        expect(storage.getSymbolAt(5)).andReturn(Optional.of(new TapeSymbol(2))).once();
        expect(storage.getSymbolAt(6)).andReturn(Optional.of(new TapeSymbol(3))).once();

        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(-3))).once();
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(2))).once();
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(3))).once();
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(6))).once();

        storage.setSymbolAt(eq(0), eq(new TapeSymbol(2)));
        expectLastCall().once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol(3)));
        expectLastCall().once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol(6)));
        expectLastCall().once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol(2)));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(4, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testADD_INDIRECT() {
        setProgram(
                instr(RamInstruction.Opcode.ADD, RamInstruction.Direction.INDIRECT, 3),
                instr(RamInstruction.Opcode.SUB, RamInstruction.Direction.INDIRECT, 4),
                instr(RamInstruction.Opcode.MUL, RamInstruction.Direction.INDIRECT, 5),
                instr(RamInstruction.Opcode.DIV, RamInstruction.Direction.INDIRECT, 6)
        );

        expect(storage.getSymbolAt(3)).andReturn(Optional.of(new TapeSymbol(8))).once();
        expect(storage.getSymbolAt(4)).andReturn(Optional.of(new TapeSymbol(9))).once();
        expect(storage.getSymbolAt(5)).andReturn(Optional.of(new TapeSymbol(10))).once();
        expect(storage.getSymbolAt(6)).andReturn(Optional.of(new TapeSymbol(11))).once();

        expect(storage.getSymbolAt(8)).andReturn(Optional.of(new TapeSymbol(5))).once();
        expect(storage.getSymbolAt(9)).andReturn(Optional.of(new TapeSymbol(-1))).once();
        expect(storage.getSymbolAt(10)).andReturn(Optional.of(new TapeSymbol(2))).once();
        expect(storage.getSymbolAt(11)).andReturn(Optional.of(new TapeSymbol(3))).once();

        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(-3))).once();
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(2))).once();
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(3))).once();
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(6))).once();

        storage.setSymbolAt(eq(0), eq(new TapeSymbol(2)));
        expectLastCall().once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol(3)));
        expectLastCall().once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol(6)));
        expectLastCall().once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol(2)));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(4, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testADD_NON_NUMERIC_OPERAND() {
        setProgram(instr(RamInstruction.Opcode.ADD, RamInstruction.Direction.CONSTANT, "not allowed"));
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(-3))).once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testADD_NON_NUMERIC_R0() {
        setProgram(instr(RamInstruction.Opcode.ADD, RamInstruction.Direction.CONSTANT, 5));
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol("haha"))).once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testADD_EMPTY_R0() {
        setProgram(instr(RamInstruction.Opcode.ADD, RamInstruction.Direction.CONSTANT, 5));
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(TapeSymbol.EMPTY)).once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol(5)));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testJMP() {
        setProgram(instr(RamInstruction.Opcode.JMP, label(100, "here")));
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(100, engine.IP.get());
    }

    @Test
    public void testJZ() {
        setProgram(instr(RamInstruction.Opcode.JZ, label(0, "here")));

        expect(storage.getSymbolAt(0)).andReturn(Optional.empty()).times(2);
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(0, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testJNZ() {
        setProgram(instr(RamInstruction.Opcode.JZ, RamInstruction.Direction.DIRECT, 0));

        expect(storage.getSymbolAt(0)).andReturn(Optional.of(TapeSymbol.guess("2"))).once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testJGTZ() {
        setProgram(instr(RamInstruction.Opcode.JGTZ, label(0, "here")));

        expect(storage.getSymbolAt(0)).andReturn(Optional.of(TapeSymbol.guess("2"))).times(2);
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(0, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testNotJGTZ() {
        setProgram(instr(RamInstruction.Opcode.JGTZ, RamInstruction.Direction.DIRECT, 0));

        expect(storage.getSymbolAt(0)).andReturn(Optional.of(TapeSymbol.EMPTY)).once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testHALT() {
        setProgram(instr(RamInstruction.Opcode.HALT, RamInstruction.Direction.DIRECT));
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.step());
        assertEquals(1, engine.IP.get());
    }

    @Test
    public void testNullInstruction() {
        expect(memory.read(0)).andReturn(null).anyTimes();
        replay(memory);
        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, engine.step());
    }

    @Test
    public void testSetInstructionLocationValid() {
        assertTrue(engine.setInstructionLocation(5));
        assertEquals(5, engine.IP.get());
    }

    @Test
    public void testSetInstructionLocationNegative() {
        assertFalse(engine.setInstructionLocation(-1));
    }

    @Test
    public void testSetInstructionLocationZero() {
        assertTrue(engine.setInstructionLocation(0));
        assertEquals(0, engine.IP.get());
    }

    @Test
    public void testLOAD_CONSTANT_INT() {
        setProgram(instr(RamInstruction.Opcode.LOAD, RamInstruction.Direction.CONSTANT, 42));
        storage.setSymbolAt(eq(0), eq(new TapeSymbol(42)));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        verify(storage);
    }

    @Test
    public void testWRITE_CONSTANT_INT() {
        setProgram(instr(RamInstruction.Opcode.WRITE, RamInstruction.Direction.CONSTANT, 99));
        output.writeData(eq(new TapeSymbol(99)));
        expectLastCall().once();
        replay(output);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        verify(output);
    }

    @Test
    public void testResetSetsIP() {
        RamMemoryContext.RamMemory snapshot = new RamMemoryContext.RamMemory(
                Collections.emptyList(), Collections.emptyMap(), Collections.emptyList()
        );
        expect(memory.getSnapshot()).andReturn(snapshot).anyTimes();
        replay(memory, input, storage, output);

        engine.reset(10);
        assertEquals(10, engine.IP.get());
    }

    @Test
    public void testResetWithInputs() {
        RamValue val1 = createNiceMock(RamValue.class);
        expect(val1.getType()).andReturn(RamValue.Type.NUMBER).anyTimes();
        expect(val1.getNumberValue()).andReturn(42).anyTimes();
        replay(val1);

        RamValue val2 = createNiceMock(RamValue.class);
        expect(val2.getType()).andReturn(RamValue.Type.STRING).anyTimes();
        expect(val2.getStringValue()).andReturn("hello").anyTimes();
        replay(val2);

        RamMemoryContext.RamMemory snapshot = new RamMemoryContext.RamMemory(
                Collections.emptyList(), Collections.emptyMap(), List.of(val1, val2)
        );
        expect(memory.getSnapshot()).andReturn(snapshot).anyTimes();
        replay(memory);

        input.clear();
        expectLastCall().once();
        input.setSymbolAt(eq(0), eq(new TapeSymbol(42)));
        expectLastCall().once();
        input.setSymbolAt(eq(1), eq(new TapeSymbol("hello")));
        expectLastCall().once();
        replay(input);

        storage.clear();
        expectLastCall().once();
        replay(storage);

        output.clear();
        expectLastCall().once();
        replay(output);

        engine.reset(0);
        verify(input, storage, output);
    }

    @Test
    public void testJZ_withEmptyString() {
        setProgram(instr(RamInstruction.Opcode.JZ, label(10, "loop")));

        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(""))).once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(10, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testJZ_withNonEmptyString() {
        setProgram(instr(RamInstruction.Opcode.JZ, RamInstruction.Direction.DIRECT, 0));

        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol("notempty"))).once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testJZ_withNullString() {
        setProgram(instr(RamInstruction.Opcode.JZ, label(10, "loop")));

        TapeSymbol symbolWithNullString = new TapeSymbol((String) null);
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(symbolWithNullString)).once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(10, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testJZ_withZeroNumber() {
        setProgram(instr(RamInstruction.Opcode.JZ, label(10, "loop")));

        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(0))).once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(10, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testJZ_withNonZeroNumber() {
        setProgram(instr(RamInstruction.Opcode.JZ, RamInstruction.Direction.DIRECT, 0));

        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(5))).once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testJGTZ_negative() {
        setProgram(instr(RamInstruction.Opcode.JGTZ, RamInstruction.Direction.DIRECT, 0));

        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(-5))).once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP.get());
        verify(storage);
    }

    @Test
    public void testREAD_CONSTANT_ignoresOperand() {
        // READ with CONSTANT direction - getRegisterNumber returns operand directly
        setProgram(instr(RamInstruction.Opcode.READ, RamInstruction.Direction.CONSTANT, 7));

        TapeSymbol symbol = new TapeSymbol("data");
        expect(input.readData()).andReturn(symbol).once();
        storage.setSymbolAt(eq(7), eq(symbol));
        expectLastCall().once();
        replay(input, storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        verify(input, storage);
    }

    @Test
    public void testSTORE_CONSTANT() {
        setProgram(instr(RamInstruction.Opcode.STORE, RamInstruction.Direction.CONSTANT, 5));

        TapeSymbol symbol = new TapeSymbol("value");
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(symbol)).once();
        storage.setSymbolAt(eq(5), eq(symbol));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        verify(storage);
    }
}
