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
package net.emustudio.plugins.cpu.ssem;

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
    private final CPU cpu;
    private final EmulatorEngine engine;
    private final CPU.CPUListener listener;

    private volatile boolean waitingForStop = false;

    AutomaticEmulation(CPU cpu, EmulatorEngine engine, MemoryContext<Byte> memory) {
        this.memory = Objects.requireNonNull(memory);
        this.engine = Objects.requireNonNull(engine);
        this.cpu = Objects.requireNonNull(cpu);

        listener = new CPU.CPUListener() {
            @Override
            public void runStateChanged(CPU.RunState runState) {
                if (runState == CPU.RunState.STATE_RUNNING) {
                    waitingForStop = true;
                } else if (waitingForStop) { // runState != STATE_RUNNING
                    waitingForStop = false;
                    snapshot();
                }
            }

            @Override
            public void internalStateChanged() {

            }
        };

        cpu.addCPUListener(listener);
    }

    void destroy() {
        cpu.removeCPUListener(listener);
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
                    String binary = RadixUtils.formatBinaryString(number, 32, 0, true);
                    writer.println(String.format("%02d %s", i, binary.replaceAll("0", "  ").replaceAll("1", "* ")));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not snapshot SSEM state", e);
        }
    }

}
