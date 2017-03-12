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
package net.sf.emustudio.zilogZ80.impl;

import emulib.plugins.cpu.DisassembledInstruction;
import emulib.plugins.cpu.Disassembler;
import emulib.runtime.exceptions.InvalidInstructionException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.jcip.annotations.ThreadSafe;
import net.sf.emustudio.intel8080.api.DispatchListener;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_C;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_H;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_N;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_PV;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_S;
import static net.sf.emustudio.zilogZ80.impl.EmulatorEngine.FLAG_Z;

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
        int PC = emulatorEngine.PC - 1;
        try {
            DisassembledInstruction instr = disassembler.disassemble(PC);

            if (useCache && !cache.contains(PC)) {
                if (numberOfMatch.get() != 0) {
                    System.out.println(String.format("%04d | Block from %04X to %04X; count=%d",
                            timeStamp, matchPC, PC, numberOfMatch.get())
                    );
                } else {
                    matchPC = PC;
                }
                numberOfMatch.set(0);
                cache.add(PC);
            } else if (useCache) {
                numberOfMatch.incrementAndGet();
            }

            if (numberOfMatch.get() <= 1) {
                System.out.print(String.format("%04d | PC=%04x | %15s | %10s ",
                        timeStamp, instr.getAddress(), instr.getMnemo(), instr.getOpCode())
                );
            }

        } catch (InvalidInstructionException e) {
            System.out.println(String.format("%04d | Invalid instruction at %04X", timeStamp, PC));
        }
    }

    @Override
    public void afterDispatch() {
        if (numberOfMatch.get() <= 1) {
            System.out.println(String.format("|| regs=%s IX=%04x IY=%04x IFF=%1x I=%02x R=%02x | flags=%s | SP=%04x | PC=%04x",
                    regsToString(), emulatorEngine.IX, emulatorEngine.IY,
                            emulatorEngine.IFF[0] ? 1 : 0,
                            emulatorEngine.I, emulatorEngine.R,
                            intToFlags(emulatorEngine.flags),
                            emulatorEngine.SP, emulatorEngine.PC)
            );
        }
    }

    private String regsToString() {
        String r = "";
        for (short i =0; i < emulatorEngine.regs.length; i++) {
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
