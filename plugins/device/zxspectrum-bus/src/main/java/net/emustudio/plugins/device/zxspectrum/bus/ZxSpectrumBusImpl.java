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

import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.api.ContextZ80;
import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;
import net.jcip.annotations.NotThreadSafe;

import java.util.*;

/**
 * ZX Spectrum bus (for 48K ZX spectrum).
 * <p>
 * Adds memory & I/O port contention.
 * <p>
 * <a href="https://sinclair.wiki.zxnet.co.uk/wiki/Contended_memory#Timing_differences">ZX Spectrum48 timing</a>
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
public class ZxSpectrumBusImpl extends AbstractMemoryContext<Byte> implements ZxSpectrumBus, CPUContext.PassedCyclesListener {
    private static final long LINE_TSTATES = 224;
    private static final long FRAME_TSTATES = 69888;

    // from 14335 to 14463, then 96 tstates pause to reach "end of line", then repeat.
    private final static Map<Long, Integer> CONTENTION_MAP = new HashMap<>();

    static {
        CONTENTION_MAP.put(14335L, 6);
        CONTENTION_MAP.put(14336L, 5);
        CONTENTION_MAP.put(14337L, 4);
        CONTENTION_MAP.put(14338L, 3);
        CONTENTION_MAP.put(14339L, 2);
        CONTENTION_MAP.put(14340L, 1);
        CONTENTION_MAP.put(14343L, 6);
        CONTENTION_MAP.put(14344L, 5);
        CONTENTION_MAP.put(14345L, 4);
        CONTENTION_MAP.put(14346L, 3);
        CONTENTION_MAP.put(14347L, 2);
        CONTENTION_MAP.put(14348L, 1);
        CONTENTION_MAP.put(14351L, 6);
        CONTENTION_MAP.put(14352L, 5);
        CONTENTION_MAP.put(14353L, 4);
        CONTENTION_MAP.put(14354L, 3);
        CONTENTION_MAP.put(14355L, 2);
        CONTENTION_MAP.put(14356L, 1);
        CONTENTION_MAP.put(14359L, 6);
        CONTENTION_MAP.put(14360L, 5);
        CONTENTION_MAP.put(14361L, 4);
        CONTENTION_MAP.put(14362L, 3);
        CONTENTION_MAP.put(14363L, 2);
        //     CONTENTION_MAP.put(14364L, 1);
    }

    private ContextZ80 cpu;
    private MemoryContext<Byte> memory;
    private volatile byte busData; // data on the bus

    private long contentionCycles;

    private final Map<Integer, Context8080.CpuPortDevice> deferredAttachments = new HashMap<>();
    private final Set<CPUContext.PassedCyclesListener> deferredListeners = new HashSet<>();

    public void initialize(ContextZ80 cpu, MemoryContext<Byte> memory) {
        this.cpu = Objects.requireNonNull(cpu);
        this.memory = Objects.requireNonNull(memory);

        for (Map.Entry<Integer, Context8080.CpuPortDevice> attachment : deferredAttachments.entrySet()) {
            if (!cpu.attachDevice(attachment.getKey(), new ContendedDeviceProxy(attachment.getValue()))) {
                throw new RuntimeException("Could not attach device " + attachment.getValue().getName() + " to CPU");
            }
        }
        for (CPUContext.PassedCyclesListener listener : deferredListeners) {
            cpu.addPassedCyclesListener(listener);
        }
        cpu.addPassedCyclesListener(this);

        deferredAttachments.clear();
        deferredListeners.clear();
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
    public byte readMemoryNotContended(int location) {
        return memory.read(location);
    }

    @Override
    public void writeMemoryNotContended(int location, byte data) {
        memory.write(location, data);
    }

    @Override
    public void addPassedCyclesListener(CPUContext.PassedCyclesListener passedCyclesListener) {
        if (cpu == null) {
            deferredListeners.add(passedCyclesListener);
        } else {
            cpu.addPassedCyclesListener(passedCyclesListener);
        }
    }

    @Override
    public void removePassedCyclesListener(CPUContext.PassedCyclesListener passedCyclesListener) {
        if (cpu == null) {
            deferredListeners.remove(passedCyclesListener);
        } else {
            cpu.removePassedCyclesListener(passedCyclesListener);
        }
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
        contendedMemory(location);
        return memory.read(location);
    }

    @Override
    public Byte[] read(int location, int count) {
        contendedMemory(location);
        return memory.read(location, count);
    }

    @Override
    public void write(int location, Byte data) {
        contendedMemory(location);
        memory.write(location, data);
    }

    @Override
    public void write(int location, Byte[] data, int count) {
        contendedMemory(location);
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

    private void contendedMemory(int location) {
        if (location >= 0x4000 && location <= 0x7FFF) {
            Integer cycles = CONTENTION_MAP.get(contentionCycles);
            if (cycles != null) {
           //     System.out.printf("%04x: %d, tstates=%d\n", location, cycles, contentionCycles);
                cpu.addCycles(cycles);
            }
        }
    }

    private void contendedPort(int portAddress) {
        //    High byte   |         |
        //    in 40 - 7F? | Low bit | Contention pattern
        //    ------------+---------+-------------------
        //         No     |  Reset  | N:1, C:3
        //         No     |   Set   | N:4
        //        Yes     |  Reset  | C:1, C:3
        //        Yes     |   Set   | C:1, C:1, C:1, C:1

        if (portAddress >= 0x4000 && portAddress <= 0x7FFF) {
            // after this, CPU adds 4 cycles for I/O.
            if ((portAddress & 1) == 0) {
                //        Yes     |  Reset  | C:1, C:3
                Integer cycles = CONTENTION_MAP.get(contentionCycles); // at C:1
                if (cycles != null) {
                    cpu.addCycles(cycles);
                }
                cycles = CONTENTION_MAP.get(contentionCycles + 1); // after C:1
                if (cycles != null) {
                    cpu.addCycles(cycles);
                }
            } else {
                //        Yes     |   Set   | C:1, C:1, C:1, C:1
                Integer cycles = CONTENTION_MAP.get(contentionCycles); // at C:1
                if (cycles != null) {
                    cpu.addCycles(cycles);
                }
                cycles = CONTENTION_MAP.get(contentionCycles + 1); // 2x at C:1
                if (cycles != null) {
                    cpu.addCycles(cycles);
                }
                cycles = CONTENTION_MAP.get(contentionCycles + 2); // 3x at C:1
                if (cycles != null) {
                    cpu.addCycles(cycles);
                }
                cycles = CONTENTION_MAP.get(contentionCycles + 3); // after 3x at C:1
                if (cycles != null) {
                    cpu.addCycles(cycles);
                }
            }
        } else {
            //         No     |  Reset  | N:1, C:3
            if ((portAddress & 1) == 0) {
                Integer cycles = CONTENTION_MAP.get(contentionCycles + 1); // after N:1
                if (cycles != null) {
                    cpu.addCycles(cycles);
                }
            }
        }
    }

    @Override
    public void passedCycles(long tstates) {
        contentionCycles = (contentionCycles + tstates) % FRAME_TSTATES;
    }

    private class ContendedDeviceProxy implements Context8080.CpuPortDevice {
        private final Context8080.CpuPortDevice device;

        private ContendedDeviceProxy(Context8080.CpuPortDevice device) {
            this.device = Objects.requireNonNull(device);
        }

        @Override
        public byte read(int portAddress) {
            contendedPort(portAddress);
            return device.read(portAddress);
        }

        @Override
        public void write(int portAddress, byte data) {
            contendedPort(portAddress);
            device.write(portAddress, data);
        }

        @Override
        public String getName() {
            return device.getName();
        }
    }
}
