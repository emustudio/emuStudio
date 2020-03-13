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

package net.emustudio.plugins.cpu.ram.impl;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.plugins.compilers.ramc.tree.RAMInstructionImpl;
import net.emustudio.plugins.cpu.ram.EmulatorEngine;
import net.emustudio.plugins.cpu.ram.api.RAMContext;
import net.emustudio.plugins.devices.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EmulatorEngineTest {

    @Test
    public void testJZwithFloatingPoint() throws Exception {
        EmulatorEngine engine = createEngineForInputTest(
            "5.5", new RAMInstructionImpl(RAMInstruction.JZ, RAMInstruction.Direction.REGISTER, 0)
        );

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(2, engine.IP);
    }

    @Test
    public void testJGTZwithFloatingPoint() throws Exception {
        EmulatorEngine engine = createEngineForInputTest(
            "5.5", new RAMInstructionImpl(RAMInstruction.JGTZ, RAMInstruction.Direction.REGISTER, 0)
        );

        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(CPU.RunState.STATE_STOPPED_BREAK, engine.step());
        assertEquals(0, engine.IP);
    }


    private EmulatorEngine createEngineForInputTest(String input, RAMInstruction testedInstruction) throws IOException {
        AbstractTapeContext storageT = mock(AbstractTapeContext.class);
        when(storageT.getSymbolAt(0)).thenReturn(input);

        AbstractTapeContext inputT = mock(AbstractTapeContext.class);
        when(inputT.readData()).thenReturn(input);

        RAMContext context = mockContext(storageT, inputT);

        return new EmulatorEngine(context, mockMemory(new RAMInstruction[]{
            new RAMInstructionImpl(RAMInstruction.READ, RAMInstruction.Direction.REGISTER, 0),
            testedInstruction
        }));
    }


    private RAMContext mockContext(AbstractTapeContext storageT, AbstractTapeContext inputT) throws IOException {
        RAMContext ramContext = mock(RAMContext.class);

        AbstractTapeContext outputT = mock(AbstractTapeContext.class);

        when(ramContext.getStorage()).thenReturn(storageT);
        when(ramContext.getInput()).thenReturn(inputT);
        when(ramContext.getOutput()).thenReturn(outputT);

        return ramContext;
    }

    private RAMMemoryContext mockMemory(RAMInstruction[] program, String... inputs) {
        RAMMemoryContext context = mock(RAMMemoryContext.class);

        when(context.getInputs()).thenReturn(Arrays.asList(inputs));
        when(context.getLabel(anyInt())).thenReturn("");

        for (int i = 0; i < program.length; i++) {
            when(context.read(i)).thenReturn(program[i]);
        }

        return context;
    }
}
