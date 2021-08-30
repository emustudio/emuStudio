package net.emustudio.plugins.cpu.ssem;

import net.emustudio.cpu.testsuite.memory.ByteMemoryStub;
import net.emustudio.emulib.plugins.cpu.DecodedInstruction;
import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertEquals;

public class DecoderTest {

    @Test
    public void testInstructionIsDecodedProperly() {
        ByteMemoryStub memory = new ByteMemoryStub(NumberUtils.Strategy.LITTLE_ENDIAN);
        memory.setMemory(new short[]{
            0, 0, 0, 0,
            0x9B, 0xE2, 0xFC, 0x3F
        });

        DecoderImpl decoder = new DecoderImpl(memory);
        DisassemblerImpl disassembler = new DisassemblerImpl(memory, decoder);
        DisassembledInstruction instr = disassembler.disassemble(4);

        assertEquals(4, instr.getAddress());
        assertEquals("LDN 27", instr.getMnemo());
        assertEquals("9B E2 FC 3F", instr.getOpCode());
    }

}
