/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.emulib.plugins.cpu.DisassembledInstruction;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.InvalidInstructionException;
import net.emustudio.plugins.cpu.intel8080.api.DispatchListener;
import net.jcip.annotations.ThreadSafe;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@ThreadSafe
public class InstructionPrinter implements DispatchListener {
    private final Disassembler disassembler;
    private final EmulatorEngine emulatorEngine;
    private final PrintStream writer;

    private final List<Integer> cache = new CopyOnWriteArrayList<>();
    private final AtomicInteger numberOfMatch = new AtomicInteger();
    private final boolean useCache;
    private volatile int matchPC;
    private volatile long creationTimeStamp;

    public InstructionPrinter(Disassembler disassembler, EmulatorEngine emulatorEngine, boolean useCache, PrintStream writer) {
        this.disassembler = Objects.requireNonNull(disassembler);
        this.emulatorEngine = Objects.requireNonNull(emulatorEngine);
        this.writer = Objects.requireNonNull(writer);
        this.useCache = useCache;
    }

    @Override
    public void beforeDispatch() {
        long timeStamp = System.currentTimeMillis() - creationTimeStamp;

        if (creationTimeStamp == 0) {
            creationTimeStamp = timeStamp;
            timeStamp = 0;
        }
        try {
            DisassembledInstruction instr = disassembler.disassemble(emulatorEngine.PC);

            if (useCache && !cache.contains(emulatorEngine.PC)) {
                if (numberOfMatch.get() != 0) {
                    writer.println(String.format("%04d | Block from %04X to %04X; count=%d",
                            timeStamp, matchPC, emulatorEngine.PC, numberOfMatch.get())
                    );
                } else {
                    matchPC = emulatorEngine.PC;
                }
                numberOfMatch.set(0);
                cache.add(emulatorEngine.PC);
            } else if (useCache) {
                numberOfMatch.incrementAndGet();
            }

            if (numberOfMatch.get() <= 1) {
                writer.print(String.format("%04d | PC=%04x | %12s | %10s ",
                        timeStamp, instr.getAddress(), instr.getMnemo(), instr.getOpCode())
                );
            }

        } catch (InvalidInstructionException e) {
            writer.println(String.format("%04d | Invalid instruction at %04X", timeStamp, emulatorEngine.PC));
        }
    }

    @Override
    public void afterDispatch() {
        if (numberOfMatch.get() <= 1) {
            writer.println(String.format("|| regs=%s | flags=%s | SP=%04x | PC=%04x",
                    regsToString(), intToFlags(emulatorEngine.flags), emulatorEngine.SP, emulatorEngine.PC)
            );
        }
    }

    private String regsToString() {
        StringBuilder r = new StringBuilder();
        for (short i = 0; i < emulatorEngine.regs.length; i++) {
            r.append(String.format("%02x ", emulatorEngine.regs[i]));
        }
        return r.toString();
    }

    private String intToFlags(int flags) {
        String flagsString = "";
        if ((flags & EmulatorEngine.FLAG_S) == EmulatorEngine.FLAG_S) {
            flagsString += "S";
        } else {
            flagsString += " ";
        }
        if ((flags & EmulatorEngine.FLAG_Z) == EmulatorEngine.FLAG_Z) {
            flagsString += "Z";
        } else {
            flagsString += " ";
        }
        if ((flags & EmulatorEngine.FLAG_AC) == EmulatorEngine.FLAG_AC) {
            flagsString += "A";
        } else {
            flagsString += " ";
        }
        if ((flags & EmulatorEngine.FLAG_P) == EmulatorEngine.FLAG_P) {
            flagsString += "P";
        } else {
            flagsString += " ";
        }
        if ((flags & EmulatorEngine.FLAG_C) == EmulatorEngine.FLAG_C) {
            flagsString += "C";
        } else {
            flagsString += " ";
        }
        return flagsString;
    }
}
