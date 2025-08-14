/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.plugins.cpu.rasp.api.RaspCpuContext;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static net.emustudio.plugins.memory.rasp.gui.Disassembler.HALT;
import static net.emustudio.plugins.memory.rasp.gui.Disassembler.JMP;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class EmulatorEngineTest {

    @Test
    public void testJumpInstruction() throws IOException {
        EmulatorEngine engine = setup(List.of(
                JMP,
                4,
                JMP,
                0,
                HALT
        ), List.of(
                new RaspLabel() {
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

    private EmulatorEngine setup(List<Integer> memoryContent, List<RaspLabel> labels) {
        AbstractTapeContext outputTape = createNiceMock(AbstractTapeContext.class);
        replay(outputTape);
        AbstractTapeContext inputTape = createNiceMock(AbstractTapeContext.class);
        replay(inputTape);

        RaspCpuContext context = createMock(RaspCpuContext.class);
        expect(context.getOutputTape()).andReturn(outputTape).anyTimes();
        expect(context.getInputTape()).andReturn(inputTape).anyTimes();
        replay(context);

        MemoryStub memory = new MemoryStub(new Annotations());
        memory.setLabels(labels);
        int address = 0;
        for (int item : memoryContent) {
            memory.write(address++, item);
        }

        return new EmulatorEngine(memory, inputTape, outputTape);
    }
}
