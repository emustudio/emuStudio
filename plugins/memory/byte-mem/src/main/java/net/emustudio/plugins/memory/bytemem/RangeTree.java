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
package net.emustudio.plugins.memory.bytemem;

import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

public class RangeTree {
    private final NavigableSet<Integer> values = new TreeSet<>();

    public void add(int from, int to) {
        for (int i = from; i <= to; i++) {
            values.add(i);
        }
    }

    public void remove(int from, int to) {
        for (int i = from; i <= to; i++) {
            values.remove(i);
        }
    }

    public List<Range> getRanges() {
        List<Range> result = new ArrayList<>();

        int from = -1;
        int to = 0;

        for (int value : values) {
            if (from == -1) {
                from = value;
                to = value;
            } else if (value - to == 1) {
                to = value;
            } else {
                result.add(new Range(from, to));
                from = value;
                to = value;
            }
        }
        if (from != -1) {
            result.add(new Range(from, to));
        }

        return result;
    }

    public boolean isIn(int value) {
        return values.contains(value);
    }

    public boolean intersects(int from, int to) {
        for (int i = from; i <= to; i++) {
            if (values.contains(i)) {
                return true;
            }
        }
        return false;
    }

    public final static class Range implements ByteMemoryContext.AddressRange {
        public final int from;
        public final int to;

        public Range(int from, int to) {
            if (from > to) {
                throw new IllegalArgumentException("FROM must be less or equal than TO");
            }
            this.from = from;
            this.to = to;
        }

        @Override
        public int getStartAddress() {
            return from;
        }

        @Override
        public int getStopAddress() {
            return to;
        }
    }
}
