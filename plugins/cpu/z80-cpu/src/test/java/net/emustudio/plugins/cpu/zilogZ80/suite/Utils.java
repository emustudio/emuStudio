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
