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

package net.emustudio.plugins.compilers.raspc.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by miso on 24.9.2017.
 */
public class Input {
    private final List<Integer> numbers = new ArrayList<>();

    public Input() {
    }

    public Input(int n) {
        numbers.add(n);
    }

    public void addNumber(int n) {
        numbers.add(n);
    }

    public List<Integer> getAll() {
        return numbers;
    }
}
