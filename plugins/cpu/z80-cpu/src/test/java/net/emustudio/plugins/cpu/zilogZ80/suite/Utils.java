/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80.suite;

import java.util.function.Predicate;

public class Utils {

    public static int get8MSBplus8LSB(int value) {
        return ((value & 0xFF00) + (byte) (value & 0xFF)) & 0xFFFF;
    }

    public static Predicate<Integer> predicate8MSBplus8LSB(int minimum) {
        return value -> get8MSBplus8LSB(value) > minimum;
    }
}
