/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.devices.mits88sio.impl;

import emulib.runtime.StaticDialogs;
import net.sf.emustudio.intel8080.api.ExtendedContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class CPUPorts {
    private final static Logger LOGGER = LoggerFactory.getLogger(CPUPorts.class);

    private final List<Integer> statusPorts = new ArrayList<>();
    private final List<Integer> dataPorts = new ArrayList<>();
    private final ExtendedContext cpu;

    CPUPorts(ExtendedContext cpu) {
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

    void reattachStatusPort(Collection<Integer> intStatusPorts, Port1 statusPort) {
        statusPorts.forEach(cpu::detachDevice);
        statusPorts.clear();
        statusPorts.addAll(intStatusPorts);

        statusPorts.stream().filter(port -> !cpu.attachDevice(statusPort, port)).forEachOrdered(port -> {
            LOGGER.error("Could not attach Status port to {}.", port);
            StaticDialogs.showErrorMessage("Could not attach Status port to " + port, "MITS SIO");
        });

    }

    void reattachDataPort(Collection<Integer> intDataPorts, Port2 dataPort) {
        dataPorts.forEach(cpu::detachDevice);
        dataPorts.clear();
        dataPorts.addAll(intDataPorts);

        dataPorts.stream().filter(port -> !cpu.attachDevice(dataPort, port)).forEachOrdered(port -> {
            LOGGER.error("Could not attach Data port to {}.", port);
            StaticDialogs.showErrorMessage("Could not attach Data port to " + port, "MITS SIO");
        });
    }


    void destroy() {
        statusPorts.forEach(cpu::detachDevice);
        dataPorts.forEach(cpu::detachDevice);
    }
}
