/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.emulib.plugins.cpu.CPUSnapshot;
import net.emustudio.emulib.plugins.cpu.CPUContext;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Objects;

class AutomaticEmulation {
    private final static Logger LOGGER = LoggerFactory.getLogger(AutomaticEmulation.class);
    private final static String SSEM_FILE_NAME = "ssem.out";

    private final MemoryContext<Byte> memory;
    private final EmulatorEngine engine;
    private final CPUContext context;
    private final CPUContext.CPUListener listener;

    private volatile boolean waitingForStop = false;

    AutomaticEmulation(CPUContext context, EmulatorEngine engine, MemoryContext<Byte> memory) {
        this.memory = MachineDebugging.inspectionMemory(Objects.requireNonNull(memory));
        this.engine = Objects.requireNonNull(engine);
        this.context = Objects.requireNonNull(context);
        listener = new CPUContext.CPUListener() {
            @Override public void runStateChanged(CPU.RunState state) {
                if (state == CPU.RunState.STATE_RUNNING) waitingForStop = true;
            }

            @Override public void internalStateChanged() {
                CPUSnapshot state = context.snapshot();
                if (state.runState() != CPU.RunState.STATE_RUNNING && !state.isBusy() && waitingForStop) {
                    waitingForStop = false;
                    snapshot();
                }
            }
        };
        context.addCPUListener(listener);
    }

    void destroy() {
        context.removeCPUListener(listener);
    }

    private void snapshot() {
        Byte[][] memorySnapshot = new Byte[memory.getSize() / 4][4];

        for (int i = 0; i < memorySnapshot.length; i++) {
            Byte[] word = memory.read(i * 4, 4);
            System.arraycopy(word, 0, memorySnapshot[i], 0, 4);
        }

        int ciSnapshot = engine.CI.get();
        int accSnapshot = engine.Acc.get();

        saveSnapshot(ciSnapshot, accSnapshot, memorySnapshot);
    }

    private void saveSnapshot(int ciSnapshot, int accSnapshot, Byte[][] memorySnapshot) {
        try (OutputStream out = new FileOutputStream(SSEM_FILE_NAME)) {
            try (PrintWriter writer = new PrintWriter(out)) {

                writer.println("ACC=0x" + Integer.toHexString(accSnapshot));
                writer.println("CI=0x" + Integer.toHexString(ciSnapshot));
                writer.println();

                writer.println("   L L L L L 5 6 7 8 9 0 1 2 I I I 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1");
                for (int i = 0; i < memorySnapshot.length; i++) {
                    int number = NumberUtils.readInt(memorySnapshot[i], NumberUtils.Strategy.BIG_ENDIAN);
                    String binary = RadixUtils.formatBinary(number, 32);
                    writer.println(String.format("%02d %s", i, binary.replace("0", "  ").replace("1", "* ")));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not snapshot SSEM state", e);
        }
    }

}
