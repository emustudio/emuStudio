/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InstructionPrinterTest {

    @Test
    public void printsElapsedTimestampsRelativeToFirstInstruction() {
        EmulatorEngine engine = createEngine();
        engine.PC = 0x1234;

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        RecordingDisassembler disassembler = new RecordingDisassembler();
        InstructionPrinter printer = new InstructionPrinter(
                disassembler, engine, false, new PrintStream(output), new SequenceClock(1000, 1015)
        );

        printer.beforeDispatch();
        printer.afterDispatch();

        engine.PC = 0x1235;
        printer.beforeDispatch();
        printer.afterDispatch();

        String[] lines = output.toString().split("\\R");
        assertEquals(2, lines.length);
        assertTrue(lines[0].startsWith("0000 | PC=1234 |"));
        assertTrue(lines[1].startsWith("0015 | PC=1235 |"));
    }

    @Test
    public void repeatedBlocksAdvanceToCurrentStartAddress() {
        EmulatorEngine engine = createEngine();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        RecordingDisassembler disassembler = new RecordingDisassembler();
        InstructionPrinter printer = new InstructionPrinter(
                disassembler, engine, true, new PrintStream(output), new SequenceClock(1000, 1001, 1002, 1003, 1004)
        );

        dispatch(printer, engine, 0x0010);
        dispatch(printer, engine, 0x0010);
        dispatch(printer, engine, 0x0020);
        dispatch(printer, engine, 0x0020);
        dispatch(printer, engine, 0x0030);

        String log = output.toString();
        assertTrue(log.contains("Block from 0010 to 0020; count=1"));
        assertTrue(log.contains("Block from 0020 to 0030; count=1"));
        assertFalse(log.contains("Block from 0010 to 0030; count=1"));
    }

    @Test
    public void negativeProgramCounterIsReportedWithoutDisassembling() {
        EmulatorEngine engine = createEngine();
        engine.PC = -1;

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        RecordingDisassembler disassembler = new RecordingDisassembler();
        InstructionPrinter printer = new InstructionPrinter(
                disassembler, engine, false, new PrintStream(output), new SequenceClock(1000)
        );

        printer.beforeDispatch();

        assertTrue(disassembler.addresses.isEmpty());
        assertEquals("0000 | Invalid instruction at FFFF", output.toString().trim());
    }

    @Test
    public void resetClampsNegativeStartPositionToZero() {
        EmulatorEngine engine = createEngine();

        engine.reset(-1);

        assertEquals(0, engine.PC);
    }

    private static EmulatorEngine createEngine() {
        MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
        return new EmulatorEngine(memory, new Context8080Impl());
    }

    private static void dispatch(InstructionPrinter printer, EmulatorEngine engine, int pc) {
        engine.PC = pc;
        printer.beforeDispatch();
        printer.afterDispatch();
    }

    private static final class RecordingDisassembler implements Disassembler {
        private final List<Integer> addresses = new ArrayList<>();

        @Override
        public DisassembledInstruction disassemble(int memoryPosition) {
            addresses.add(memoryPosition);
            return new DisassembledInstruction(memoryPosition, "NOP", "00");
        }

        @Override
        public int getNextInstructionPosition(int memoryPosition) {
            return memoryPosition + 1;
        }
    }

    private static final class SequenceClock implements LongSupplier {
        private final long[] values;
        private int index;

        private SequenceClock(long... values) {
            this.values = values;
        }

        @Override
        public long getAsLong() {
            int currentIndex = Math.min(index, values.length - 1);
            index++;
            return values[currentIndex];
        }
    }
}
