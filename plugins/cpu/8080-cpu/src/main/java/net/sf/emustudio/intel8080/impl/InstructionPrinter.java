package net.sf.emustudio.intel8080.impl;

import emulib.plugins.cpu.DisassembledInstruction;
import emulib.plugins.cpu.Disassembler;
import emulib.plugins.cpu.InvalidInstructionException;
import net.jcip.annotations.ThreadSafe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@ThreadSafe
public class InstructionPrinter implements EmulatorEngine.DispatchListener {
    private final Disassembler disassembler;
    private final EmulatorEngine emulatorEngine;

    private final List<Integer> cache = new CopyOnWriteArrayList<>();
    private final AtomicInteger numberOfMatch = new AtomicInteger();
    private final AtomicInteger matchPC = new AtomicInteger();

    public InstructionPrinter(Disassembler disassembler, EmulatorEngine emulatorEngine) {
        this.disassembler = Objects.requireNonNull(disassembler);
        this.emulatorEngine = Objects.requireNonNull(emulatorEngine);
    }

    @Override
    public void beforeDispatch() {
        try {
            DisassembledInstruction instr = disassembler.disassemble(emulatorEngine.PC);

            if (!cache.contains(emulatorEngine.PC)) {
                if (numberOfMatch.get() != 0) {
                    System.out.println(String.format("Block from %04X to %04X; count=%d",
                            matchPC.get(), emulatorEngine.PC, numberOfMatch.get())
                    );
                }
                numberOfMatch.set(0);
                matchPC.set(emulatorEngine.PC);
                cache.add(emulatorEngine.PC);
            } else {
                numberOfMatch.incrementAndGet();
            }

            if (numberOfMatch.get() <= 1) {
                System.out.print(String.format("%04x | %12s | %10s ",
                        instr.getAddress(), instr.getMnemo(), instr.getOpCode())
                );
            }

        } catch (InvalidInstructionException e) {
            System.out.println(String.format("Invalid instruction at %04X", emulatorEngine.PC));
        }
    }

    @Override
    public void afterDispatch() {
        if (numberOfMatch.get() <= 1) {
            System.out.println(String.format("|| regs=%s | flags=%02x | SP=%04x | PC=%04x",
                    regsToString(), emulatorEngine.flags, emulatorEngine.SP, emulatorEngine.PC)
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

}
