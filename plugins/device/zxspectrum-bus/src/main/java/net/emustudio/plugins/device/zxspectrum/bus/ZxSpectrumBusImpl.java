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

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.cpu.TimedEventsProcessor;
import net.emustudio.emulib.plugins.memory.AbstractMemoryContext;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.MemoryContextAnnotations;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.cpu.zilogZ80.api.ContextZ80;
import net.emustudio.plugins.device.zxspectrum.bus.api.ZxSpectrumBus;
import net.jcip.annotations.NotThreadSafe;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Also a memory proxy
 */
@NotThreadSafe
public class ZxSpectrumBusImpl extends AbstractMemoryContext<Byte> implements ZxSpectrumBus {
    private ContextZ80 cpu;
    private MemoryContext<Byte> memory;
    private volatile byte busData; // data on the bus

    private final Map<Integer, Context8080.CpuPortDevice> deferredAttachments = new HashMap<>();


    public void initialize(ContextZ80 cpu, MemoryContext<Byte> memory) {
        this.cpu = Objects.requireNonNull(cpu);
        this.memory = Objects.requireNonNull(memory);

        for (Map.Entry<Integer, Context8080.CpuPortDevice> attachment : deferredAttachments.entrySet()) {
            // TODO: contended device proxy if needed
            if (!cpu.attachDevice(attachment.getKey(), attachment.getValue())) {
                throw new RuntimeException("Could not attach device " + attachment.getValue().getName() + " to CPU");
            }
        }
    }

    @Override
    public void attachDevice(int port, Context8080.CpuPortDevice device) {
        if (cpu == null) {
            deferredAttachments.put(port, device);
        } else {
            // TODO: contended device proxy if needed
            if (!cpu.attachDevice(port, device)) {
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
        // TODO: contention
        return memory.read(location);
    }

    @Override
    public Byte[] read(int location, int count) {
        // TODO: contention
        return memory.read(location, count);
    }

    @Override
    public void write(int location, Byte data) {
        // TODO: contention
        memory.write(location, data);
    }

    @Override
    public void write(int location, Byte[] data, int count) {
        // TODO: contention
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
}
