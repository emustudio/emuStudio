/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh;

import net.emustudio.plugins.device.mits88tap.api.PaperTapeContext;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

public class PaperTapeCommandsIntegrationTest {
    private PseudoContext context;
    private ByteMemoryContext memory;
    private PaperTapeContext tape;

    @Before
    public void setUp() {
        memory = createMock(ByteMemoryContext.class);
        tape = createMock(PaperTapeContext.class);
        context = new PseudoContext();
        context.setMemory(memory);
        context.setPaperTape(tape);
        context.reset();
    }

    @Test
    public void testAttachAndResetReaderThroughPortProtocol() throws Exception {
        expectCommandLine("reader.pt");
        tape.attachReader(Paths.get("reader.pt"));
        expectLastCall().once();
        tape.rewindReader();
        expectLastCall().once();
        replay(memory, tape);

        context.write(0xFE, (byte) Commands.attachPTRCmd.ordinal());
        assertEquals(0, context.read(0xFE));
        context.write(0xFE, (byte) Commands.resetPTRCmd.ordinal());

        verify(memory, tape);
    }

    @Test
    public void testAttachAndDetachPunchThroughPortProtocol() throws Exception {
        expectCommandLine("punch.pt");
        tape.attachPunch(Paths.get("punch.pt"));
        expectLastCall().once();
        tape.detachPunch();
        expectLastCall().once();
        replay(memory, tape);

        context.write(0xFE, (byte) Commands.attachPTPCmd.ordinal());
        assertEquals(0, context.read(0xFE));
        context.write(0xFE, (byte) Commands.detachPTPCmd.ordinal());

        verify(memory, tape);
    }

    private void expectCommandLine(String value) {
        expect(memory.read(0x80)).andReturn((byte) (value.length() + 1));
        for (int i = 0; i < value.length(); i++) {
            expect(memory.read(0x82 + i)).andReturn((byte) value.charAt(i));
        }
    }
}
