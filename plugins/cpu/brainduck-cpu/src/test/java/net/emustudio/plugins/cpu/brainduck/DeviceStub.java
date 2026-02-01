/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck;

import net.emustudio.emulib.plugins.annotations.PluginContext;
import net.emustudio.emulib.plugins.device.DeviceContext;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@PluginContext
public class DeviceStub implements DeviceContext<Byte> {
    private final List<Byte> output = new CopyOnWriteArrayList<>();
    private final Queue<Byte> input = new ConcurrentLinkedQueue<>();

    public void setInput(byte[] input) {
        for (byte value : input) {
            this.input.add(value);
        }
    }

    @Override
    public Byte readData() {
        return input.poll();
    }

    @Override
    public void writeData(Byte data) {
        output.add(data);
    }

    @Override
    public Class<Byte> getDataType() {
        return Byte.class;
    }

    public boolean wasInputRead() {
        return input.isEmpty();
    }

    public List<Byte> getOutput() {
        return output;
    }
}
