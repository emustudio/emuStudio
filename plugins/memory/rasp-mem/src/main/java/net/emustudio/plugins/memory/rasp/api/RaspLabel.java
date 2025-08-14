/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp.api;

import java.io.Serializable;

/**
 * A Label is a named pointer to an address in memory.
 */
public interface RaspLabel extends Serializable {

    /**
     * Get address to which this label points to
     *
     * @return memory address
     */
    int getAddress();

    /**
     * Get name of this label
     *
     * @return name of this label
     */
    String getLabel();
}
