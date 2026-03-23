/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

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

import static net.emustudio.plugins.cpu.zilogZ80.EmulatorEngine.*;

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
        int PC = emulatorEngine.PC - 1;
        try {
            DisassembledInstruction instr = disassembler.disassemble(PC);

            if (useCache && !cache.contains(PC)) {
                if (numberOfMatch.get() != 0) {
                    writer.printf("%04d | Block from %04X to %04X; count=%d%n", timeStamp, matchPC, PC, numberOfMatch.get());
                } else {
                    matchPC = PC;
                }
                numberOfMatch.set(0);
                cache.add(PC);
            } else if (useCache) {
                numberOfMatch.incrementAndGet();
            }

            if (numberOfMatch.get() <= 1) {
                writer.printf("%04d | PC=%04x | %15s | %10s ", timeStamp, instr.address, instr.mnemo, instr.opCode);
            }
        } catch (InvalidInstructionException e) {
            writer.printf("%04d | Invalid instruction at %04X%n", timeStamp, PC);
        }
    }

    @Override
    public void afterDispatch() {
        if (numberOfMatch.get() <= 1) {
            writer.printf("|| regs=%s IX=%04x IY=%04x IFF=%1x I=%02x R=%02x | flags=%s | SP=%04x | PC=%04x%n",
                    regsToString(), emulatorEngine.IX, emulatorEngine.IY,
                    emulatorEngine.IFF[0] ? 1 : 0,
                    emulatorEngine.I, emulatorEngine.R,
                    intToFlags(emulatorEngine.flags),
                    emulatorEngine.SP, emulatorEngine.PC);
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
        if ((flags & FLAG_S) == FLAG_S) {
            flagsString += "S";
        } else {
            flagsString += " ";
        }
        if ((flags & FLAG_Z) == FLAG_Z) {
            flagsString += "Z";
        } else {
            flagsString += " ";
        }
        if ((flags & FLAG_H) == FLAG_H) {
            flagsString += "H";
        } else {
            flagsString += " ";
        }
        if ((flags & FLAG_PV) == FLAG_PV) {
            flagsString += "P";
        } else {
            flagsString += " ";
        }
        if ((flags & FLAG_N) == FLAG_N) {
            flagsString += "N";
        } else {
            flagsString += " ";
        }
        if ((flags & FLAG_C) == FLAG_C) {
            flagsString += "C";
        } else {
            flagsString += " ";
        }
        return flagsString;
    }
}
