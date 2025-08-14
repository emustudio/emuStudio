/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp;

/**
 * Breakpoint in debugger; it is an exception, so, can be thrown.
 *
 * @author miso
 */
public class Breakpoint extends Exception {

    public Breakpoint() {
    }

}
