package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.memory.rasp.InstructionImpl;
import net.emustudio.plugins.memory.rasp.NumberMemoryItem;
import net.emustudio.plugins.memory.rasp.api.MemoryItem;
import net.emustudio.plugins.memory.rasp.api.RASPInstruction;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class EmulatorEngineTest {

    @Test
    public void testJumpInstruction() throws IOException {
        EmulatorEngine engine = setup(List.of(
            new InstructionImpl(RASPInstruction.JMP),
            new NumberMemoryItem(4),
            new InstructionImpl(RASPInstruction.JMP),
            new NumberMemoryItem(0),
            new InstructionImpl(RASPInstruction.HALT),
            new NumberMemoryItem(0)
        ), Map.of(
            4, "HERE"
        ));

        engine.reset(0);
        engine.step();
        CPU.RunState state = engine.step();

        assertEquals(CPU.RunState.STATE_STOPPED_NORMAL, state);
    }






    private EmulatorEngine setup(List<MemoryItem> items, Map<Integer, String> labels) {
        AbstractTapeContext outputTape = createNiceMock(AbstractTapeContext.class);
        replay(outputTape);
        AbstractTapeContext inputTape = createNiceMock(AbstractTapeContext.class);
        replay(inputTape);

        RASPCpuContext context = createMock(RASPCpuContext.class);
        expect(context.getOutputTape()).andReturn(outputTape).anyTimes();
        expect(context.getInputTape()).andReturn(inputTape).anyTimes();
        replay(context);

        RaspMemoryStub memory = new RaspMemoryStub(items, Collections.emptyList(), labels);
        Dialogs dialogs = createNiceMock(Dialogs.class);

        return new EmulatorEngine(context, memory, dialogs);
    }
}
