package net.sf.emustudio.memory.standard.impl;

import net.sf.emustudio.memory.standard.StandardMemoryContext;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

public class RangeTree {
    private final NavigableSet<Integer> values = new TreeSet<>();

    public void add(int from, int to) {
        for (int i = from; i < to; i++) {
            values.add(i);
        }
    }

    public void remove(int from, int to) {
        for (int i = from; i < to; i++) {
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

        return result;
    }

    public boolean isIn(int value) {
        return values.contains(value);
    }

    public final static class Range implements StandardMemoryContext.AddressRange {
        public final int from;
        public final int to;

        public Range(int from, int to) {
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
