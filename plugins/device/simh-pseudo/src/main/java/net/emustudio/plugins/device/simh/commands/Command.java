/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.simh.commands;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

public interface Command {

    /**
     * Called on SIMH interface reset
     */
    default void reset(Control control) {

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

        Context8080 getCpu();
    }


}
