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

package net.emustudio.plugins.cpu.ram;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import org.junit.Test;

import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class EmulatorEngineTest extends AbstractEngineTest {

    @Test
    public void testREAD_DIRECT() {
        setProgram(instr(RAMInstruction.Opcode.READ, RAMInstruction.Direction.DIRECT, 5));

        TapeSymbol symbol = new TapeSymbol("hello");
        expect(input.readData()).andReturn(symbol).once();
        storage.setSymbolAt(eq(5), eq(symbol));
        expectLastCall().once();
        replay(input, storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP);
        verify(input, storage);
    }

    @Test
    public void testREAD_INDIRECT() {
        setProgram(instr(RAMInstruction.Opcode.READ, RAMInstruction.Direction.INDIRECT, 3));

        TapeSymbol symbol = new TapeSymbol("hello");
        expect(input.readData()).andReturn(symbol).once();
        expect(storage.getSymbolAt(3)).andReturn(Optional.of(new TapeSymbol(5))).once();
        storage.setSymbolAt(eq(5), eq(symbol));
        expectLastCall().once();
        replay(input, storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP);
        verify(input, storage);
    }

    @Test
    public void testWRITE_CONSTANT() {
        setProgram(instr(RAMInstruction.Opcode.WRITE, RAMInstruction.Direction.CONSTANT, "yoohoo"));
        output.writeData(eq(new TapeSymbol("yoohoo")));
        expectLastCall().once();
        replay(output);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP);
        verify(output);
    }

    @Test
    public void testWRITE_DIRECT() {
        setProgram(instr(RAMInstruction.Opcode.WRITE, RAMInstruction.Direction.DIRECT, 3));

        expect(storage.getSymbolAt(3)).andReturn(Optional.of(new TapeSymbol("yoohoo"))).once();
        output.writeData(eq(new TapeSymbol("yoohoo")));
        expectLastCall().once();
        replay(storage, output);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP);
        verify(storage, output);
    }

    @Test
    public void testWRITE_INDIRECT() {
        setProgram(instr(RAMInstruction.Opcode.WRITE, RAMInstruction.Direction.INDIRECT, 3));

        expect(storage.getSymbolAt(3)).andReturn(Optional.of(new TapeSymbol(5))).once();
        expect(storage.getSymbolAt(5)).andReturn(Optional.of(new TapeSymbol("yoohoo"))).once();
        output.writeData(eq(new TapeSymbol("yoohoo")));
        expectLastCall().once();
        replay(storage, output);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP);
        verify(storage, output);
    }

    @Test
    public void testLOAD_CONSTANT() {
        setProgram(instr(RAMInstruction.Opcode.LOAD, RAMInstruction.Direction.CONSTANT, "yoohoo"));
        storage.setSymbolAt(eq(0), eq(new TapeSymbol("yoohoo")));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP);
        verify(storage);
    }

    @Test
    public void testLOAD_DIRECT() {
        setProgram(instr(RAMInstruction.Opcode.LOAD, RAMInstruction.Direction.DIRECT, 3));
        expect(storage.getSymbolAt(3)).andReturn(Optional.of(new TapeSymbol("yoohoo"))).once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol("yoohoo")));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP);
        verify(storage);
    }

    @Test
    public void testLOAD_INDIRECT() {
        setProgram(instr(RAMInstruction.Opcode.LOAD, RAMInstruction.Direction.INDIRECT, 5));
        expect(storage.getSymbolAt(5)).andReturn(Optional.of(new TapeSymbol(3))).once();
        expect(storage.getSymbolAt(3)).andReturn(Optional.of(new TapeSymbol("yoohoo"))).once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol("yoohoo")));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP);
        verify(storage);
    }

    @Test
    public void testSTORE_DIRECT() {
        setProgram(instr(RAMInstruction.Opcode.STORE, RAMInstruction.Direction.DIRECT, 5));

        TapeSymbol symbol = new TapeSymbol("yoohoo");
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(symbol)).once();
        storage.setSymbolAt(eq(5), eq(symbol));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP);
        verify(storage);
    }

    @Test
    public void testSTORE_INDIRECT() {
        setProgram(instr(RAMInstruction.Opcode.STORE, RAMInstruction.Direction.INDIRECT, 3));

        TapeSymbol symbol = new TapeSymbol("yoohoo");
        expect(storage.getSymbolAt(3)).andReturn(Optional.of(new TapeSymbol(5))).once();
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(symbol)).once();
        storage.setSymbolAt(eq(5), eq(symbol));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP);
        verify(storage);
    }

    @Test
    public void testArith_CONSTANT() {
        setProgram(
            instr(RAMInstruction.Opcode.ADD, RAMInstruction.Direction.CONSTANT, 5),
            instr(RAMInstruction.Opcode.SUB, RAMInstruction.Direction.CONSTANT, -1),
            instr(RAMInstruction.Opcode.MUL, RAMInstruction.Direction.CONSTANT, 2),
            instr(RAMInstruction.Opcode.DIV, RAMInstruction.Direction.CONSTANT, 3)
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
        assertEquals(4, engine.IP);
        verify(storage);
    }

    @Test
    public void testADD_DIRECT() {
        setProgram(
            instr(RAMInstruction.Opcode.ADD, RAMInstruction.Direction.DIRECT, 3),
            instr(RAMInstruction.Opcode.SUB, RAMInstruction.Direction.DIRECT, 4),
            instr(RAMInstruction.Opcode.MUL, RAMInstruction.Direction.DIRECT, 5),
            instr(RAMInstruction.Opcode.DIV, RAMInstruction.Direction.DIRECT, 6)
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
        assertEquals(4, engine.IP);
        verify(storage);
    }

    @Test
    public void testADD_INDIRECT() {
        setProgram(
            instr(RAMInstruction.Opcode.ADD, RAMInstruction.Direction.INDIRECT, 3),
            instr(RAMInstruction.Opcode.SUB, RAMInstruction.Direction.INDIRECT, 4),
            instr(RAMInstruction.Opcode.MUL, RAMInstruction.Direction.INDIRECT, 5),
            instr(RAMInstruction.Opcode.DIV, RAMInstruction.Direction.INDIRECT, 6)
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
        assertEquals(4, engine.IP);
        verify(storage);
    }

    @Test(expected = RuntimeException.class)
    public void testADD_NON_NUMERIC_OPERAND() {
        setProgram(instr(RAMInstruction.Opcode.ADD, RAMInstruction.Direction.CONSTANT, "not allowed"));
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol(-3))).once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, engine.step());
        assertEquals(1, engine.IP);
        verify(storage);
    }

    @Test(expected = RuntimeException.class)
    public void testADD_NON_NUMERIC_R0() {
        setProgram(instr(RAMInstruction.Opcode.ADD, RAMInstruction.Direction.CONSTANT, 5));
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(new TapeSymbol("haha"))).once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BAD_INSTR, engine.step());
        assertEquals(1, engine.IP);
        verify(storage);
    }

    @Test
    public void testADD_EMPTY_R0() {
        setProgram(instr(RAMInstruction.Opcode.ADD, RAMInstruction.Direction.CONSTANT, 5));
        expect(storage.getSymbolAt(0)).andReturn(Optional.of(TapeSymbol.EMPTY)).once();
        storage.setSymbolAt(eq(0), eq(new TapeSymbol(5)));
        expectLastCall().once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP);
        verify(storage);
    }

    @Test
    public void testJMP() {
        setProgram(instr(RAMInstruction.Opcode.JMP, label(100, "here")));
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(100, engine.IP);
    }

    @Test
    public void testJZ() {
        setProgram(instr(RAMInstruction.Opcode.JZ, label(0, "here")));

        expect(storage.getSymbolAt(0)).andReturn(Optional.empty()).times(2);
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(0, engine.IP);
        verify(storage);
    }

    @Test
    public void testJNZ() {
        setProgram(instr(RAMInstruction.Opcode.JZ, RAMInstruction.Direction.DIRECT, 0));

        expect(storage.getSymbolAt(0)).andReturn(Optional.of(TapeSymbol.guess("2"))).once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP);
        verify(storage);
    }

    @Test
    public void testJGTZ() {
        setProgram(instr(RAMInstruction.Opcode.JGTZ, label(0, "here")));

        expect(storage.getSymbolAt(0)).andReturn(Optional.of(TapeSymbol.guess("2"))).times(2);
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(0, engine.IP);
        verify(storage);
    }

    @Test
    public void testNotJGTZ() {
        setProgram(instr(RAMInstruction.Opcode.JGTZ, RAMInstruction.Direction.DIRECT, 0));

        expect(storage.getSymbolAt(0)).andReturn(Optional.of(TapeSymbol.EMPTY)).once();
        replay(storage);

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(1, engine.IP);
        verify(storage);
    }

    @Test
    public void testHALT() {
        setProgram(instr(RAMInstruction.Opcode.HALT, RAMInstruction.Direction.DIRECT));
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.step());
        assertEquals(1, engine.IP);
    }
}
