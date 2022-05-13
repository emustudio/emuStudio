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
package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.plugins.cpu.rasp.api.RASPCpuContext;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.memory.rasp.api.RASPLabel;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryCell;
import org.junit.Test;

import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class EmulatorEngineTest {

    @Test
    public void testJumpInstruction() {
        EmulatorEngine engine = setup(List.of(
            RASPCell.instruction(0, 15),
            RASPCell.operand(1, 4),
            RASPCell.instruction(2, 15),
            RASPCell.operand(3, 0),
            RASPCell.instruction(4, 18)
        ), List.of(
            new RASPLabel() {
                @Override
                public int getAddress() {
                    return 4;
                }

                @Override
                public String getLabel() {
                    return "HERE";
                }
            }
        ));

        engine.reset(0);
        engine.step();
        CPU.RunState state = engine.step();

        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, state);
    }

    private EmulatorEngine setup(List<RASPMemoryCell> items, List<RASPLabel> labels) {
        AbstractTapeContext outputTape = createNiceMock(AbstractTapeContext.class);
        replay(outputTape);
        AbstractTapeContext inputTape = createNiceMock(AbstractTapeContext.class);
        replay(inputTape);

        RASPCpuContext context = createMock(RASPCpuContext.class);
        expect(context.getOutputTape()).andReturn(outputTape).anyTimes();
        expect(context.getInputTape()).andReturn(inputTape).anyTimes();
        replay(context);

        MemoryStub memory = new MemoryStub();
        memory.setLabels(labels);
        for (RASPMemoryCell item : items) {
            memory.write(item.getAddress(), item);
        }

        return new EmulatorEngine(memory, inputTape, outputTape);
    }
}
