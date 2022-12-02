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
package net.emustudio.plugins.device.mits88sio;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SIO "unit" is basically a device capable of communicating with:
 * - 8080/Z80 CPU from one side according to the protocol described in 88-SIO manual
 * - Any Byte device from the other side
 * <p>
 * A SIO unit allocates one or two CPU ports (status, data).
 * A device is connected with SIO unit using special port.
 */
public class SioUnit implements AutoCloseable {
    private final SioUnitSettings settings;
    private final UART uart;
    private final ControlChannel controlChannel;
    private final DataChannel dataChannel;
    private final Context8080 cpu;

    private final List<Integer> attachedStatusPorts = new ArrayList<>();
    private final List<Integer> attachedDataPorts = new ArrayList<>();

    public SioUnit(SioUnitSettings settings, Context8080 cpu) {
        this.settings = Objects.requireNonNull(settings);
        this.cpu = Objects.requireNonNull(cpu);
        this.uart = new UART(cpu, settings);
        this.controlChannel = new ControlChannel(uart);
        this.dataChannel = new DataChannel(settings, uart);
    }

    void reset(boolean guiSupported) {
        uart.reset(guiSupported);
    }

    public UART getUART() {
        return uart;
    }

    public void attach() {
        detach();
        attachedDataPorts.clear();
        attachedStatusPorts.clear();

        attachedStatusPorts.addAll(settings.getStatusPorts());
        attachedDataPorts.addAll(settings.getDataPorts());

        attachedStatusPorts.forEach(p -> cpu.attachDevice(controlChannel, p));
        attachedDataPorts.forEach(p -> cpu.attachDevice(dataChannel, p));
    }

    public void detach() {
        attachedStatusPorts.forEach(cpu::detachDevice);
        attachedDataPorts.forEach(cpu::detachDevice);
    }

    @Override
    public void close() {
        detach();
    }
}
