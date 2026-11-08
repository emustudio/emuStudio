/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import org.junit.Test;
import java.util.*;
import static net.emustudio.plugins.memory.rasp.gui.Disassembler.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DebugHooksTest {
    @Test public void recordsIntegerWordsAndPreservesNegativeValues() throws Exception {
        MemoryStub memory = new MemoryStub(new Annotations());
        memory.write(0, 0);
        memory.write(5, 4); // LOAD constant
        memory.write(6, -300);
        memory.write(7, HALT);
        AbstractTapeContext input = createNiceMock(AbstractTapeContext.class);
        AbstractTapeContext output = createNiceMock(AbstractTapeContext.class);
        replay(input, output);
        EmulatorEngine engine = new EmulatorEngine(memory, input, output);
        engine.IP.set(5);
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
        assertEquals(5, events.get(0).address);
        assertTrue(events.get(0).opcodes.contains(Integer.toHexString(-300)));
        assertTrue(events.get(0).mnemonic.contains("-300"));
        assertEquals(Long.valueOf(Integer.toUnsignedLong(-300)), events.get(0).after.get("ACC"));
        assertTrue(events.get(0).accesses.stream().anyMatch(a -> a.write && a.address == 0 && a.value == -300));
        assertTrue(events.get(0).accesses.stream().anyMatch(a -> !a.write && a.address == 6 && a.value == -300));
        assertSame(memory, engine.debugger.memory());
    }

    @Test public void refusedInstructionDoesNotFetchOrAdvance() throws Exception {
        MemoryStub memory = new MemoryStub(new Annotations());
        memory.write(0, HALT);
        AbstractTapeContext input = createNiceMock(AbstractTapeContext.class);
        AbstractTapeContext output = createNiceMock(AbstractTapeContext.class);
        replay(input, output);
        EmulatorEngine engine = new EmulatorEngine(memory, input, output);
        engine.debugger.addListener(new DebugCPU.Listener() {
            @Override public boolean beforeInstruction(int address, int bank) { return true; }
        });
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(0, engine.IP.get());
        assertTrue(engine.debugStopped);
    }
}
