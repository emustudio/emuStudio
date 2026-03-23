/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DebugTableModelImplTest {

    @Test
    public void breakpointColumnIsNotEditableForRowsWithoutInstructionLocation() {
        Disassembler disassembler = new LinearDisassembler();
        CPU cpu = mock(CPU.class);
        when(cpu.getDisassembler()).thenReturn(disassembler);
        when(cpu.getInstructionLocation()).thenReturn(0);
        when(cpu.isBreakpointSupported()).thenReturn(true);
        when(cpu.isBreakpointSet(anyInt())).thenReturn(false);

        DebugTableModelImpl model = new DebugTableModelImpl();
        model.setCPU(cpu, () -> 32);

        assertFalse(model.isCellEditable(0, 0));
        assertTrue(model.isCellEditable(PaginatingDisassembler.INSTR_PER_PAGE / 2, 0));
    }

    private static final class LinearDisassembler implements Disassembler {
        @Override
        public DisassembledInstruction disassemble(int memoryPosition) {
            return new DisassembledInstruction(memoryPosition, "nop", "00");
        }

        @Override
        public int getNextInstructionPosition(int memoryPosition) {
            return memoryPosition + 1;
        }
    }
}
