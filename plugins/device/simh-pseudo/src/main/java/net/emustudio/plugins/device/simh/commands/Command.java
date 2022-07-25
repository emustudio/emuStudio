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
package net.emustudio.plugins.device.simh.commands;

import net.emustudio.emulib.plugins.device.DeviceContext;
import net.emustudio.plugins.cpu.intel8080.api.ExtendedContext;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.util.HashMap;
import java.util.Map;

public interface Command {

    /**
     * Called on SIMH interface reset
     */
    default void reset() {

    }

    /**
     * Read byte
     *
     * @param control control
     * @return data
     */
    default byte read(Control control) {
        return 0;
    }

    /**
     * Write data byte
     *
     * @param data    byte
     * @param control control
     */
    default void write(byte data, Control control) {

    }

    /**
     * On command start
     *
     * @param control control
     */
    default void start(Control control) {

    }


    interface Control {

        /**
         * Clears last command
         */
        void clearCommand();

        void clearReadCommand();

        void clearWriteCommand();

        ByteMemoryContext getMemory();

        ExtendedContext getCpu();

        DeviceContext<Byte> getDevice();
    }


}
