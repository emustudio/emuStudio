/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.debug.DebugCPU;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class DebugHooksTest {
    @Test public void recordsActualFetchAddressAndRegistersDuringRun() {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[128]);
        memory.write(1, (byte) 3); // CMP skips the following instruction when ACC is negative.
        memory.write(9, (byte) 7); // STP at byte address 8.
        EmulatorEngine engine = new EmulatorEngine(memory, address -> false);
        engine.reset(-4);
        engine.Acc.set(-1);
        List<DebugCPU.Instruction> events = new ArrayList<>();
        engine.debugger.addListener(new DebugCPU.Listener() {
            @Override public boolean afterInstruction(DebugCPU.Instruction event) {
                events.add(event);
                return false;
            }
        });
        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, engine.run());
        assertEquals(2, events.size());
        assertEquals(0, events.get(0).address);
        assertEquals(8, events.get(1).address);
        assertTrue(events.get(0).mnemonic.toLowerCase().contains("cmp"));
        assertFalse(events.get(0).opcodes.isBlank());
        assertEquals(Long.valueOf(0xffffffffL), events.get(0).before.get("ACC"));
        assertEquals(Long.valueOf(8), events.get(0).after.get("PC"));
        assertTrue(events.get(0).accesses.stream().anyMatch(a -> !a.write && a.address == 0));
        assertEquals(1, events.get(0).sequence);
        assertEquals(2, events.get(1).sequence);
    }

    @Test public void stopBeforeInstructionLeavesCiAndAccumulatorUntouched() {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[128]);
        EmulatorEngine engine = new EmulatorEngine(memory, address -> false);
        engine.reset(-4);
        engine.debugger.addListener(new DebugCPU.Listener() {
            @Override public boolean beforeInstruction(int address) { return true; }
        });
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(-4, engine.CI.get());
        assertEquals(0, engine.Acc.get());
    }
    @Test public void repeatedAddressUsesCurrentCodeAfterMutationAndReset() {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.REVERSE_BITS);
        memory.setMemory(new short[128]);
        memory.write(1, (byte) 3); // CMP.
        EmulatorEngine engine = new EmulatorEngine(memory, address -> false);
        List<DebugCPU.Instruction> events = new ArrayList<>();
        engine.debugger.addListener(new DebugCPU.Listener() {
            @Override public boolean afterInstruction(DebugCPU.Instruction event) {
                events.add(event);
                return false;
            }
        });
        engine.reset(-4);
        engine.step();
        memory.write(1, (byte) 7); // Replace same address with STP.
        engine.reset(-4);
        engine.step();
        assertTrue(events.get(0).mnemonic.toLowerCase().contains("cmp"));
        assertTrue(events.get(1).mnemonic.toLowerCase().contains("stp"));
        assertNotEquals(events.get(0).opcodes, events.get(1).opcodes);
        assertEquals(2, events.get(1).sequence);
    }
}
