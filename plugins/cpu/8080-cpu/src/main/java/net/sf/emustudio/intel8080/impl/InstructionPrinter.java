/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.intel8080.impl;

import emulib.plugins.cpu.DisassembledInstruction;
import emulib.plugins.cpu.Disassembler;
import emulib.runtime.exceptions.InvalidInstructionException;
import net.jcip.annotations.ThreadSafe;
import net.sf.emustudio.intel8080.api.DispatchListener;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static net.sf.emustudio.intel8080.impl.EmulatorEngine.*;

@ThreadSafe
public class InstructionPrinter implements DispatchListener {
    private final Disassembler disassembler;
    private final EmulatorEngine emulatorEngine;

    private final List<Integer> cache = new CopyOnWriteArrayList<>();
    private final AtomicInteger numberOfMatch = new AtomicInteger();
    private volatile int matchPC;
    private final boolean useCache;

    private volatile long creationTimeStamp;

    public InstructionPrinter(Disassembler disassembler, EmulatorEngine emulatorEngine, boolean useCache) {
        this.disassembler = Objects.requireNonNull(disassembler);
        this.emulatorEngine = Objects.requireNonNull(emulatorEngine);
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
                    System.out.println(String.format("%04d | Block from %04X to %04X; count=%d",
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
                System.out.print(String.format("%04d | PC=%04x | %12s | %10s ",
                    timeStamp, instr.getAddress(), instr.getMnemo(), instr.getOpCode())
                );
            }

        } catch (InvalidInstructionException e) {
            System.out.println(String.format("%04d | Invalid instruction at %04X", timeStamp, emulatorEngine.PC));
        }
    }

    @Override
    public void afterDispatch() {
        if (numberOfMatch.get() <= 1) {
            System.out.println(String.format("|| regs=%s | flags=%s | SP=%04x | PC=%04x",
                regsToString(), intToFlags(emulatorEngine.flags), emulatorEngine.SP, emulatorEngine.PC)
            );
        }
    }

    private String regsToString() {
        String r = "";
        for (short i = 0; i < emulatorEngine.regs.length; i++) {
            r += String.format("%02x ", emulatorEngine.regs[i]);
        }
        return r;
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
        if ((flags & FLAG_AC) == FLAG_AC) {
            flagsString += "A";
        } else {
            flagsString += " ";
        }
        if ((flags & FLAG_P) == FLAG_P) {
            flagsString += "P";
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
