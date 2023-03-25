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
package net.emustudio.plugins.cpu.intel8080.api;

import net.jcip.annotations.ThreadSafe;

import java.util.Objects;

@ThreadSafe
public class FrequencyUpdater implements Runnable {

    private final CpuEngine cpu;
    private long startTimeSaved = 0;
    private float frequency;

    public FrequencyUpdater(CpuEngine cpu) {
        this.cpu = Objects.requireNonNull(cpu);
    }

    @Override
    public void run() {
        boolean frequencyChanged = false;

        synchronized (this) {
            long endTime = System.nanoTime();
            long time = endTime - startTimeSaved;
            long executedCycles = cpu.getAndResetExecutedCycles();

            if (executedCycles > 0) {
                frequency = (float) (executedCycles / (time / 1000000.0));
                startTimeSaved = endTime;
                frequencyChanged = true;
            }
        }

        if (frequencyChanged) {
            cpu.fireFrequencyChanged(frequency);
        }
    }
}
