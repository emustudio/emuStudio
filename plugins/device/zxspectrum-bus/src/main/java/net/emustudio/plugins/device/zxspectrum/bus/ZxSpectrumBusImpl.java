/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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

import static net.emustudio.plugins.device.zxspectrum.bus.api.ZxParameters.*;

/**
 * ZX Spectrum bus (for 48K ZX spectrum).
 * <p>
 * - Adds memory & I/O port contention
 * - Adds "floating bus" behavior
 * - Sets CPU interrupt request to last 32 t-states
 * <p>It is implemented as memory context, because "bus" behavior is also reading/writing data on specific address.
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
    private static final int IO_PORTS = 0x100;
    private static final int SCREEN_FETCH_CYCLES = ATTRIBUTES_WIDTH * 4; // 128

    // First contended T-state after interrupt. Each screen line has 128 contended T-states + 96 non-contended.
    private static final long FIRST_CONTENDED = 14335;
    private static final long FIRST_FLOATING_BUS = FIRST_CONTENDED + 3; // 14338 on 48K
    private final static Map<Long, Integer> CONTENTION_MAP = new HashMap<>();

    static {
        // 192 screen lines, each with 128 T-states of contention (16 repetitions of 6,5,4,3,2,1,0,0)
        // followed by 96 T-states of no contention (border/retrace).
        for (int line = 0; line < SCREEN_HEIGHT_PIXELS; line++) {
            long lineStart = FIRST_CONTENDED + line * DISPLAY_LINE_TSTATES;
            for (long j = 0; j < SCREEN_FETCH_CYCLES; j += 8) {
                CONTENTION_MAP.put(lineStart + j, 6);
                CONTENTION_MAP.put(lineStart + j + 1, 5);
                CONTENTION_MAP.put(lineStart + j + 2, 4);
                CONTENTION_MAP.put(lineStart + j + 3, 3);
                CONTENTION_MAP.put(lineStart + j + 4, 2);
                CONTENTION_MAP.put(lineStart + j + 5, 1);
            }
        }
    }

    private ContextZ80 cpu;
    private MemoryContext<Byte> memory;
    private volatile byte busData; // data on the bus

    private long frameCycles;

    private final Context8080.CpuPortDevice[] attachedDevices = new Context8080.CpuPortDevice[IO_PORTS];
    private final Map<Integer, Context8080.CpuPortDevice> deferredAttachments = new HashMap<>();
    private final Set<CPUContext.PassedCyclesListener> deferredListeners = new HashSet<>();

    public void initialize(ContextZ80 cpu, MemoryContext<Byte> memory) {
        this.cpu = Objects.requireNonNull(cpu);
        this.memory = Objects.requireNonNull(memory);

        // ZX Spectrum ULA holds INT low for 32 T-states at each frame boundary
        cpu.setInterruptDuration(INTERRUPT_TSTATES);

        attachPortDispatchers();

        for (Map.Entry<Integer, Context8080.CpuPortDevice> attachment : deferredAttachments.entrySet()) {
            registerDevice(attachment.getKey(), attachment.getValue());
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
        Context8080.CpuPortDevice checked = Objects.requireNonNull(device);
        int lowPort = port & 0xFF;
        if (cpu == null) {
            Context8080.CpuPortDevice old = deferredAttachments.putIfAbsent(lowPort, checked);
            if (old != null) {
                throw new RuntimeException("Could not attach device " + checked.getName() + " to CPU; port already taken by " + old.getName());
            }
        } else {
            registerDevice(lowPort, checked);
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
    public void clearInterrupt() {
        cpu.clearInterrupt();
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
        contendMemory(location);
        return memory.read(location);
    }

    @Override
    public Byte[] read(int location, int count) {
        contendMemory(location);
        return memory.read(location, count);
    }

    @Override
    public void write(int location, Byte data) {
        contendMemory(location);
        memory.write(location, data);
    }

    @Override
    public void write(int location, Byte[] data, int count) {
        contendMemory(location);
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

    private void contendMemory(int location) {
        if (location >= 0x4000 && location <= 0x7FFF) {
            Integer cycles = contentionDelayAt(frameCycles);
            if (cycles != null) {
                cpu.addCycles(cycles);
            }
        }
    }

    private void registerDevice(int lowPort, Context8080.CpuPortDevice device) {
        Context8080.CpuPortDevice old = attachedDevices[lowPort];
        if (old != null) {
            throw new RuntimeException("Could not attach device " + device.getName() + " to CPU; port already taken by " + old.getName());
        }
        attachedDevices[lowPort] = device;
    }

    private void attachPortDispatchers() {
        for (int lowPort = 0; lowPort < IO_PORTS; lowPort++) {
            if (!cpu.attachDevice(lowPort, new PortDispatcher(lowPort))) {
                throw new RuntimeException("Could not attach ZX Spectrum bus dispatcher to CPU port " + lowPort);
            }
        }
    }

    private Integer contentionDelayAt(long cycle) {
        long normalized = cycle % DISPLAY_FRAME_TSTATES;
        if (normalized < 0) {
            normalized += DISPLAY_FRAME_TSTATES;
        }
        return CONTENTION_MAP.get(normalized);
    }

    @Override
    public void passedCycles(long tstates) {
        frameCycles = (frameCycles + tstates) % DISPLAY_FRAME_TSTATES;
    }

    /**
     * Handles all I/O accesses whose low address byte matches {@code lowPort}.
     * <p>
     * The CPU API exposes one device slot per low byte, so this bus installs one dispatcher for each value from
     * {@code 0x00} to {@code 0xFF}. The dispatcher still receives the original 16-bit {@code portAddress}, applies
     * the Spectrum contention rules for that full address, then either forwards the access to the registered device
     * or models an unclaimed port: floating-bus on reads, ignored writes on writes.
     */
    private class PortDispatcher implements Context8080.CpuPortDevice {
        // Floating-bus reads observe the ULA value on the last T-state of the 4T I/O read cycle.
        // By the time readIO() reaches this bus dispatcher, frameCycles already points at the start of that cycle,
        // so sampling the ULA fetch position requires a +3 T-state offset.
        private static final int IO_READ_SAMPLE_OFFSET = 3;
        private final int lowPort;

        private PortDispatcher(int lowPort) {
            this.lowPort = lowPort;
        }

        @Override
        public byte read(int portAddress) {
            contendedPort(portAddress);
            Context8080.CpuPortDevice device = attachedDevices[lowPort];
            if (device != null) {
                return device.read(portAddress);
            }
            long sampleCycle = (frameCycles + IO_READ_SAMPLE_OFFSET) % DISPLAY_FRAME_TSTATES;
            return readFloatingBus(sampleCycle);
        }

        @Override
        public void write(int portAddress, byte data) {
            contendedPort(portAddress);
            Context8080.CpuPortDevice device = attachedDevices[lowPort];
            if (device != null) {
                device.write(portAddress, data);
            }
        }

        @Override
        public String getName() {
            return "ZX-Spectrum bus port dispatcher";
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
                    Integer cycles = contentionDelayAt(frameCycles); // at C:1
                    if (cycles != null) {
                        cpu.addCycles(cycles);
                    }
                    cycles = contentionDelayAt(frameCycles + 1); // after C:1
                    if (cycles != null) {
                        cpu.addCycles(cycles);
                    }
                } else {
                    //        Yes     |   Set   | C:1, C:1, C:1, C:1
                    Integer cycles = contentionDelayAt(frameCycles); // at C:1
                    if (cycles != null) {
                        cpu.addCycles(cycles);
                    }
                    cycles = contentionDelayAt(frameCycles + 1); // 2x at C:1
                    if (cycles != null) {
                        cpu.addCycles(cycles);
                    }
                    cycles = contentionDelayAt(frameCycles + 2); // 3x at C:1
                    if (cycles != null) {
                        cpu.addCycles(cycles);
                    }
                    cycles = contentionDelayAt(frameCycles + 3); // after 3x at C:1
                    if (cycles != null) {
                        cpu.addCycles(cycles);
                    }
                }
            } else {
                //         No     |  Reset  | N:1, C:3
                if ((portAddress & 1) == 0) {
                    Integer cycles = contentionDelayAt(frameCycles + 1); // after N:1
                    if (cycles != null) {
                        cpu.addCycles(cycles);
                    }
                }
            }
        }


        /**
         * Reads the ZX Spectrum floating-bus value for a given frame-relative sample cycle.
         * <p>
         * During active ULA fetch phases, this returns the screen or attribute byte currently driven by the ULA.
         * Outside the visible fetch window, the bus is treated as undriven and this returns {@code 0xFF}.
         *
         * @param sampleCycle frame-relative cycle at which IN contention samples the floating bus
         * @return value observed on the floating bus at {@code sampleCycle}, or {@code 0xFF} when no ULA fetch is active
         */
        private byte readFloatingBus(long sampleCycle) {
            long visibleCycles = sampleCycle - FIRST_FLOATING_BUS;
            if (visibleCycles < 0) {
                return (byte) 0xFF;
            }

            int line = (int) (visibleCycles / DISPLAY_LINE_TSTATES);
            if (line < 0 || line >= SCREEN_HEIGHT_PIXELS) {
                return (byte) 0xFF;
            }

            int cycleInLine = (int) (visibleCycles % DISPLAY_LINE_TSTATES);
            if (cycleInLine >= SCREEN_FETCH_CYCLES) {
                return (byte) 0xFF;
            }

            int column = (cycleInLine / 8) * 2;
            int phase = cycleInLine & 7;
            switch (phase) {
                case 0:
                    return readScreenByte(line, column);
                case 1:
                    return readAttributeByte(line, column);
                case 2:
                    return readScreenByte(line, column + 1);
                case 3:
                    return readAttributeByte(line, column + 1);
                default:
                    return (byte) 0xFF;
            }
        }

        private byte readScreenByte(int line, int column) {
            int lineOffset = ((line & 0xC0) << 5) | ((line & 7) << 8) | ((line & 0x38) << 2);

            // non-contended read
            return memory.read(0x4000 + lineOffset + column);
        }

        private byte readAttributeByte(int line, int column) {
            int attributeOffset = ((line >>> 3) << 5) | column;

            // non-contended read
            return memory.read(0x5800 + attributeOffset);
        }
    }
}
