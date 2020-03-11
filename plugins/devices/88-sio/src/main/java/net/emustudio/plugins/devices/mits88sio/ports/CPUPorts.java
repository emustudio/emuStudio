/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.devices.mits88sio.ports;

import net.emustudio.plugins.cpu.intel8080.api.ExtendedContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CPUPorts {
    private final List<Integer> statusPorts = new ArrayList<>();
    private final List<Integer> dataPorts = new ArrayList<>();
    private final ExtendedContext cpu;

    public CPUPorts(ExtendedContext cpu) {
        this.cpu = Objects.requireNonNull(cpu);
    }

    public int getStatusPortsCount() {
        return statusPorts.size();
    }

    public int getDataPortsCount() {
        return dataPorts.size();
    }

    public int getStatusPort(int index) {
        return statusPorts.get(index);
    }

    public int getDataPort(int index) {
        return dataPorts.get(index);
    }

    public void reattachStatusPort(Collection<Integer> intStatusPorts, StatusPort statusPort) throws CouldNotAttachException {
        statusPorts.forEach(cpu::detachDevice);
        statusPorts.clear();
        statusPorts.addAll(intStatusPorts);

        List<Integer> unattachedPorts = statusPorts.stream()
            .filter(port -> !cpu.attachDevice(statusPort, port))
            .collect(Collectors.toList());

        if (!unattachedPorts.isEmpty()) {
            throw new CouldNotAttachException("Could not attach Status port to " + unattachedPorts);
        }
    }

    public void reattachDataPort(Collection<Integer> intDataPorts, DataPort dataPort) throws CouldNotAttachException {
        dataPorts.forEach(cpu::detachDevice);
        dataPorts.clear();
        dataPorts.addAll(intDataPorts);

        List<Integer> unattachedPorts = dataPorts.stream()
            .filter(port -> !cpu.attachDevice(dataPort, port))
            .collect(Collectors.toList());

        if (!unattachedPorts.isEmpty()) {
            throw new CouldNotAttachException("Could not attach Data port to " + unattachedPorts);
        }
    }

    public void destroy() {
        statusPorts.forEach(cpu::detachDevice);
        dataPorts.forEach(cpu::detachDevice);
    }
}
