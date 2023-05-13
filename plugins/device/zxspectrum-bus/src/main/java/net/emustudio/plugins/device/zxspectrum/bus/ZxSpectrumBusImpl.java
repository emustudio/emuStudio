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
package net.emustudio.plugins.device.zxspectrum.bus;

import net.emustudio.emulib.plugins.cpu.TimedEventsProcessor;
import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.api.ContextZ80;
import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;
import net.jcip.annotations.NotThreadSafe;

import java.util.*;
import java.util.function.Consumer;

/**
 * ZX Spectrum bus (for 48K ZX spectrum).
 * <p>
 * Adds memory & I/O port contention.
 * <p>
 * https://sinclair.wiki.zxnet.co.uk/wiki/Contended_memory#Timing_differences
 * On the 16K and 48K models of ZX Spectrum, the memory from 0x4000 to 0x7fff is contended. If the contended
 * memory is accessed 14335[2] or 14336 tstates after an interrupt (see the timing differences section below
 * for information on the 14335/14336 issue), the Z80 will be delayed for 6 tstates. After 14336 tstates,
 * the delay is 5 tstates:
 * <p>
 * Cycle #    Delay
 * -------    -----
 * 14335       6 (until 14341)
 * 14336       5 (  "     "  )
 * 14337       4 (  "     "  )
 * 14338       3 (  "     "  )
 * 14339       2 (  "     "  )
 * 14340       1 (  "     "  )
 * 14341   No delay
 * 14342   No delay
 * 14343       6 (until 14349)
 * 14344       5 (  "     "  )
 * 14345       4 (  "     "  )
 * 14346       3 (  "     "  )
 * 14347       2 (  "     "  )
 * 14348       1 (  "     "  )
 * 14349   No delay
 * 14350   No delay
 * <p>
 * This pattern (6,5,4,3,2,1,0,0) continues until 14463 tstates after interrupt, at which point there is no
 * delay for 96 tstates while the border and horizontal refresh are drawn. The pattern starts again at 14559
 * tstates and continues for all 192 lines of screen data. After this, there is no delay until the end of the
 * frame as the bottom border and vertical refresh happen, and no delay until 14335 tstates after the start of
 * the next frame as the top border is drawn.
 * <p>
 * Contended I/O
 * High byte   |         |
 * in 40 - 7F? | Low bit | Contention pattern
 * ------------+---------+-------------------
 * No          |  Reset  | N:1, C:3
 * No          |   Set   | N:4
 * Yes         |  Reset  | C:1, C:3
 * Yes         |   Set   | C:1, C:1, C:1, C:1
 */
@NotThreadSafe
public class ZxSpectrumBusImpl extends AbstractMemoryContext<Byte> implements ZxSpectrumBus {
    private final static int SCREEN_LINES = 192;
    private final static int CONTENTION_TSTATE_START = 14335;
    private static final int CPU_INTERRUPT_TSTATES = 69888;

    private ContextZ80 cpu;
    private MemoryContext<Byte> memory;
    private volatile byte busData; // data on the bus
    private TimedEventsProcessor tep;
    private boolean isContended = false;

    private final Map<Integer, Context8080.CpuPortDevice> deferredAttachments = new HashMap<>();

    public void initialize(ContextZ80 cpu, MemoryContext<Byte> memory) {
        this.cpu = Objects.requireNonNull(cpu);
        this.memory = Objects.requireNonNull(memory);
        this.tep = cpu.getTimedEventsProcessor().orElseThrow(() -> new RuntimeException("CPU must provide TimedEventProcessor"));

        for (Map.Entry<Integer, Context8080.CpuPortDevice> attachment : deferredAttachments.entrySet()) {
            if (!cpu.attachDevice(attachment.getKey(), new ContendedDeviceProxy(attachment.getValue()))) {
                throw new RuntimeException("Could not attach device " + attachment.getValue().getName() + " to CPU");
            }
        }

        //  17 x from 14335 to 14463, then 96 tstates pause to reach "end of line", then repeat.
        // the t-state 14335 is the first screen line to be read.
        int cycles = CONTENTION_TSTATE_START + CPU_INTERRUPT_TSTATES;
        for (int line = 0; line < SCREEN_LINES; line++) {
            scheduleMemoryContention(cycles);
            cycles = cycles + 17 * 8 + 96 - 1;
        }
    }


    @Override
    public void attachDevice(int port, Context8080.CpuPortDevice device) {
        if (cpu == null) {
            deferredAttachments.put(port, device);
        } else {
            // TODO: contended device proxy if needed
            if (!cpu.attachDevice(port, new ContendedDeviceProxy(device))) {
                throw new RuntimeException("Could not attach device " + device.getName() + " to CPU");
            }
        }
    }

    @Override
    public void signalNonMaskableInterrupt() {
        cpu.signalNonMaskableInterrupt();
    }

    @Override
    public void signalInterrupt(byte[] data) {
        cpu.signalInterrupt(data);
    }

    @Override
    public Optional<TimedEventsProcessor> getTimedEventsProcessor() {
        return cpu.getTimedEventsProcessor();
    }

    @Override
    public byte readMemoryNotContended(int location) {
        return memory.read(location);
    }

    @Override
    public void writeMemoryNotContended(int location, byte data) {
        memory.write(location, data);
    }

    @Override
    public Byte readData() {
        return busData;
    }

    @Override
    public void writeData(Byte data) {
        this.busData = data;
    }

    @Override
    public Byte read(int location) {
        setContended(location);
        return memory.read(location);
    }

    @Override
    public Byte[] read(int location, int count) {
        setContended(location);
        return memory.read(location, count);
    }

    @Override
    public void write(int location, Byte data) {
        setContended(location);
        memory.write(location, data);
    }

    @Override
    public void write(int location, Byte[] data, int count) {
        setContended(location);
        memory.write(location, data, count);
    }

    @Override
    public Class<Byte> getCellTypeClass() {
        return Byte.class;
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
    }

    @Override
    public void clear() {
        memory.clear();
    }

    @Override
    public int getSize() {
        return memory.getSize();
    }

    @Override
    public MemoryContextAnnotations annotations() {
        return memory.annotations();
    }

    private void setContended(int location) {
        this.isContended = location >= 0x4000 && location <= 0x7FFF;
    }

    private void scheduleMemoryContention(int startCycles) {
        //  17 x from 14335 to 14463, then 96 tstates pause to reach "end of line", then repeat.
        Runnable slowDownByOneCycle = () -> {
            if (isContended) {
                cpu.addCycles(1);
            }
        };

        for (int i = 0; i < 17; i++) {
            for (int j = 0; j < 6; j++) {
                tep.schedule(startCycles + i * 8 + j, slowDownByOneCycle);
            }
        }
    }

    private class ContendedDeviceProxy implements Context8080.CpuPortDevice {
        private final Context8080.CpuPortDevice device;

        private ContendedDeviceProxy(Context8080.CpuPortDevice device) {
            this.device = Objects.requireNonNull(device);
        }

        @Override
        public byte read(int portAddress) {
            setContended(portAddress);
            // TODO: portAddress & 1 == 0  ==> contended for 3 cycles only
            return device.read(portAddress);
        }

        @Override
        public void write(int portAddress, byte data) {
            setContended(portAddress);
            // TODO: portAddress & 1 == 0  ==> contended for 3 cycles only
            device.write(portAddress, data);
        }

        @Override
        public String getName() {
            return device.getName();
        }
    }
}
