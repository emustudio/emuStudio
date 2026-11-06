/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import org.junit.Test;
import java.util.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DebugHooksTest extends AbstractEngineTest {
    @Test public void recordsTypedInstructionAndStringOperandWithoutByteMemory() {
        setProgram(instr(RamInstruction.Opcode.WRITE, RamInstruction.Direction.CONSTANT, "hello"),
                instr(RamInstruction.Opcode.HALT, RamInstruction.Direction.DIRECT));
        output.writeData(eq(new TapeSymbol("hello")));
        expectLastCall().once();
        output.moveRight();
        expectLastCall().once();
        replay(input, storage, output);
        List<DebugCPU.Instruction> events = new ArrayList<>();
        engine.debugger.addListener(new DebugCPU.Listener() {
            @Override public boolean afterInstruction(DebugCPU.Instruction event) {
                events.add(event);
                return false;
            }
        });
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.step());
        assertEquals(2, events.size());
        assertEquals(0, events.get(0).address);
        assertEquals(Long.valueOf(1), events.get(0).after.get("PC"));
        assertTrue(events.get(0).mnemonic.contains("HELLO"));
        assertEquals("WRITE CONSTANT hello", events.get(0).opcodes);
        assertTrue(events.get(0).accesses.isEmpty()); // Typed program/tapes are not byte memory.
        assertSame(memory, engine.debugger.memory());
        verify(output);
    }

    @Test public void refusedInstructionDoesNotAdvanceTypedProgramCounter() {
        setProgram(instr(RamInstruction.Opcode.HALT, RamInstruction.Direction.DIRECT));
        engine.debugger.addListener(new DebugCPU.Listener() {
            @Override public boolean beforeInstruction(int address) { return true; }
        });
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(0, engine.IP.get());
        assertTrue(engine.debugStopped);
    }
}
