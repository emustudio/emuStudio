/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import net.emustudio.plugins.device.mits88tap.api.PaperTapeContext;
import org.junit.Before;

import static org.easymock.EasyMock.*;

/**
 * Base class for command unit tests providing mock infrastructure.
 */
public abstract class CommandTestBase {
    protected Command.Control control;
    protected Context8080 cpu;
    protected ByteMemoryContext memory;
    protected PaperTapeContext paperTape;

    private boolean commandCleared;
    private boolean readCommandCleared;
    private boolean writeCommandCleared;

    @Before
    public void setUpBase() {
        cpu = createNiceMock(Context8080.class);
        memory = createNiceMock(ByteMemoryContext.class);
        paperTape = createNiceMock(PaperTapeContext.class);

        commandCleared = false;
        readCommandCleared = false;
        writeCommandCleared = false;

        control = new Command.Control() {
            @Override
            public void clearCommand() {
                commandCleared = true;
            }

            @Override
            public void clearReadCommand() {
                readCommandCleared = true;
            }

            @Override
            public void clearWriteCommand() {
                writeCommandCleared = true;
            }

            @Override
            public ByteMemoryContext getMemory() {
                return memory;
            }

            @Override
            public Context8080 getCpu() {
                return cpu;
            }

            @Override
            public PaperTapeContext getPaperTape() {
                return paperTape;
            }
        };
    }

    protected void expectCommandLine(String commandLine) {
        expect(memory.read(0x80)).andReturn((byte) (commandLine.length() + 1));
        for (int i = 0; i < commandLine.length(); i++) {
            expect(memory.read(0x82 + i)).andReturn((byte) commandLine.charAt(i));
        }
    }

    protected boolean isCommandCleared() {
        return commandCleared;
    }

    protected boolean isReadCommandCleared() {
        return readCommandCleared;
    }

    protected boolean isWriteCommandCleared() {
        return writeCommandCleared;
    }

    protected void resetClearFlags() {
        commandCleared = false;
        readCommandCleared = false;
        writeCommandCleared = false;
    }
}
