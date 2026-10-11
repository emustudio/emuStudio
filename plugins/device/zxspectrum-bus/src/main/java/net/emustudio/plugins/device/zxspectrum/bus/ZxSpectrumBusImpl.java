/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.bus;

import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.api.ContextZ80;
import net.emustudio.plugins.device.zxspectrum.bus.api.ModelProfile;
import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;
import net.jcip.annotations.NotThreadSafe;

import java.util.*;

/**
 * Connects the ZX Spectrum CPU, memory, and devices.
 *
 * <p>The active {@link ModelProfile} supplies memory and I/O contention, floating-bus
 * sampling, and interrupt timing. This memory context proxies CPU accesses to the backing
 * byte memory while keeping cycle counters and device attachments local to the machine.
 */
@NotThreadSafe
public class ZxSpectrumBusImpl extends AbstractMemoryContext<Byte> implements ZxSpectrumBus, CPUContext.PassedCyclesListener {
    private static final int IO_PORTS = 0x100;

    private final ModelProfile profile;
    private final int displayLineTstates;
    private final int displayFrameTstates;
    private final long firstFloatingBusTstate;

    private ContextZ80 cpu;
    private MemoryContext<Byte> memory;
    // Last tape bit driven onto the bus by the cassette player.
    private volatile byte busData;
    // True only while the tape deck actively drives EAR. When false, port 0xFE bit 6 must read
    // idle low without injecting another tape edge into the timing stream.
    private volatile boolean tapeEarLineDriven;

    private long frameCycles;

    private final Context8080.CpuPortDevice[] attachedDevices = new Context8080.CpuPortDevice[IO_PORTS];
    private final Map<Integer, Context8080.CpuPortDevice> deferredAttachments = new HashMap<>();
    private final Set<CPUContext.PassedCyclesListener> deferredListeners = new HashSet<>();

    public ZxSpectrumBusImpl() {
        this(ModelProfile.DEFAULT);
    }

    public ZxSpectrumBusImpl(ModelProfile profile) {
        this.profile = Objects.requireNonNull(profile);
        this.displayLineTstates = profile.displayLineTstates;
        this.displayFrameTstates = profile.displayFrameTstates;
        this.firstFloatingBusTstate = profile.firstFloatingBusTstate;
    }

    public void initialize(ContextZ80 cpu, MemoryContext<Byte> memory) {
        this.cpu = Objects.requireNonNull(cpu);
        this.memory = Objects.requireNonNull(memory);
        this.busData = 0;
        this.tapeEarLineDriven = false;

        // Configure the CPU interrupt pulse from the active model.
        cpu.setInterruptDuration(profile.interruptTstates);

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
    public ModelProfile getProfile() {
        return profile;
    }

    public void reset() {
        // The ULA frame/interrupt clock also restarts at zero when the computer is reset.
        frameCycles = 0;
    }

    @Override
    public boolean attachDevice(int port, Context8080.CpuPortDevice device) {
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
        return true;
    }

    @Override
    public void detachDevice(int port) {
        deferredAttachments.remove(port);
        attachedDevices[port] = null;
    }

    @Override
    public void setCPUFrequency(int freq) {
        ContextZ80 cpu = this.cpu;
        if (cpu != null) {
            cpu.setCPUFrequency(freq);
        }
    }

    @Override
    public void signalNonMaskableInterrupt() {
        cpu.signalNonMaskableInterrupt();
    }

    @Override
    public void addCycles(long tStates) {
        cpu.addCycles(tStates);
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
    public void passedCycles(int location, int cycles) {
        int maskedLocation = location & 0xFFFF;
        for (int i = 0; i < cycles; i++) {
            applyMemoryContention(maskedLocation);
        }
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
    public int getCPUFrequency() {
        if (cpu == null) {
            throw new IllegalStateException("ZX Spectrum bus is not initialized");
        }
        return cpu.getCPUFrequency();
    }

    @Override
    public Byte readData() {
        return tapeEarLineDriven ? busData : (byte) 0;
    }

    @Override
    public void writeData(Byte data) {
        this.busData = data;
        this.tapeEarLineDriven = true;
    }

    public void releaseTapeEarLine() {
        this.tapeEarLineDriven = false;
    }

    @Override
    public Byte read(int location) {
        applyMemoryContention(location);
        return memory.read(location);
    }

    @Override
    public Byte[] read(int location, int count) {
        applyMemoryContention(location);
        return memory.read(location, count);
    }

    @Override
    public void write(int location, Byte data) {
        applyMemoryContention(location);
        memory.write(location, data);
    }

    @Override
    public void write(int location, Byte[] data, int count) {
        applyMemoryContention(location);
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

    public void destroy() {
        ContextZ80 tmp = cpu;
        if (tmp != null) {
            tmp.removePassedCyclesListener(this);
        }
        deferredAttachments.clear();
        Arrays.fill(attachedDevices, null);
    }

    private void applyMemoryContention(int location) {
        if (profile.isContendedMemoryAddress(location)) {
            int cycles = profile.contentionDelayAt(frameCycles);
            if (cycles > 0) {
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

    @Override
    public void passedCycles(long tstates) {
        frameCycles = (frameCycles + tstates) % displayFrameTstates;
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
            applyPortContention(portAddress);
            Context8080.CpuPortDevice device = attachedDevices[lowPort];
            if (device != null) {
                return device.read(portAddress);
            }
            // applyPortContention() advances frameCycles through cpu.addCycles(), so sample from
            // the already-delayed clock position instead of adding the same wait states twice.
            long sampleCycle = (frameCycles + IO_READ_SAMPLE_OFFSET) % displayFrameTstates;
            return readFloatingBus(sampleCycle);
        }

        @Override
        public void write(int portAddress, byte data) {
            applyPortContention(portAddress);
            Context8080.CpuPortDevice device = attachedDevices[lowPort];
            if (device != null) {
                device.write(portAddress, data);
            }
        }

        @Override
        public String getName() {
            return "ZX-Spectrum Bus";
        }

        private void applyPortContention(int portAddress) {
            int cycles = profile.portContentionDelay(frameCycles, portAddress);
            if (cycles > 0) {
                cpu.addCycles(cycles);
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
            long visibleCycles = sampleCycle - firstFloatingBusTstate;
            if (visibleCycles < 0) {
                return (byte) profile.undrivenBusDataByte;
            }

            int line = (int) (visibleCycles / displayLineTstates);
            if (line < 0 || line >= profile.screenHeightPixels) {
                return (byte) profile.undrivenBusDataByte;
            }

            int cycleInLine = (int) (visibleCycles % displayLineTstates);
            if (!profile.isFloatingBusDrivenAtCycle(cycleInLine)) {
                return (byte) profile.undrivenBusDataByte;
            }

            int column = profile.floatingBusColumnAt(cycleInLine);
            int floatingBusAddress = profile.isFloatingBusAttributePhase(cycleInLine)
                    ? profile.attributeAddressAt(line, column)
                    : profile.screenAddressAt(line, column);

            return memory.read(floatingBusAddress);
        }
    }
}
