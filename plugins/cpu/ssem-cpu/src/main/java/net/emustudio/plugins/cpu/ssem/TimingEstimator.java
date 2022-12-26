/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.helpers.NumberUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class TimingEstimator {
    private final short[] memory = new short[]{
            0x06, 0xA4, 0x41, 0x04,
            0x9B, 0xF2, 0x20, 0x88,
            0x82, 0x16, 0x88, 0x50,
            0x02, 0x13, 0x42, 0x60,
            0xEB, 0xF1, 0xAA, 0x94,
            0x80, 0xC1, 0x10, 0xA9,
            0x81, 0xE1, 0x09, 0x0C,
            0x81, 0xE1, 0x06, 0x02,
            0x98, 0x06, 0x86, 0x41,
            0xA9, 0xE2, 0x49, 0x02,
            0x01, 0xE3, 0x34, 0x84,
            0x69, 0xE1, 0x30, 0x48,
            0xE9, 0xE1, 0x48, 0x30,
            0xA8, 0xC6, 0x84, 0x30,
            0xA1, 0xE1, 0x02, 0x48,
            0x13, 0xF6, 0x01, 0x84,
            0x07, 0xF9, 0x00, 0x82,
            0x83, 0xF6, 0xFF, 0xFF,
            0xA9, 0xE2, 0x66, 0x66,
            0xA8, 0xC6, 0xFF, 0xFF,
            0x18, 0xC0, 0xFF, 0xFF,
            0x60, 0x00, 0x00, 0x00,
            0xE0, 0x00, 0x00, 0x00,
            0xFF, 0xFF, 0xFF, 0xFF,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x01, 0xF0, 0x20,
            0x00, 0x02, 0x08, 0x50,
            0x00, 0x02, 0x28, 0x20,
            0x01, 0xFA, 0x08, 0x38,
            0x02, 0x01, 0xF0, 0x20,
            0x1E, 0x78, 0x80, 0x20,
            0x3F, 0xFF, 0xE0, 0x50
    };

    private final MemoryContext<Byte> memoryContext = new AbstractMemoryContext<>() {

        @Override
        public Byte read(int position) {
            return (byte) memory[position];
        }

        @Override
        public Byte[] read(int position, int length) {
            return NumberUtils.nativeShortsToBytes(Arrays.copyOfRange(memory, position, position + length));
        }

        @Override
        public void write(int position, Byte value) {
            memory[position] = value;
        }

        @Override
        public void write(int position, Byte[] bytes, int length) {
            System.arraycopy(bytes, 0, memory, position, length);
        }

        @Override
        public Class<Byte> getDataType() {
            return Byte.class;
        }

        @Override
        public void clear() {

        }

        @Override
        public int getSize() {
            return memory.length;
        }
    };

    public long estimateWaitNanos(int instructionsPerSecond) {
        EmulatorEngine engine = new EmulatorEngine(memoryContext, position -> false);

        int testInstructionsCount = 5000;
        assert (testInstructionsCount > instructionsPerSecond);
        double coeff = (double) instructionsPerSecond / testInstructionsCount;

        long start = System.nanoTime();
        for (int i = 0; i < testInstructionsCount; i++) {
            engine.step();
        }
        long t1 = System.nanoTime() - start;

        // we want 700 per second
        // instructions go much faster on host computer
        // we estimate how long takes X instructions to perform over time
        // we now time T1 .... X instructions  (assuming X > 700)
        //             T2 .... 700 instructions
        // ------------------------------------
        // T2 : T1 = 700 : X
        // T2 = T1 * 700 / X

        double t2 = t1 * coeff;
        assert (t1 > t2);


        // T2 is the real time needed to perform 700 instructions, it it much less than 1 second
        // we want 700 instructions/second
        //
        // so after 700 instructions we should wait 1 second - T2
        // If we wanted to wait after each instruction (more smooth), then:
        // wait = (1 second - T2) / 700

        return (long) ((TimeUnit.SECONDS.toNanos(1) - t2) / instructionsPerSecond);
    }
}
