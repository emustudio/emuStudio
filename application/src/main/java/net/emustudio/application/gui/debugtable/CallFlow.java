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
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

@ThreadSafe
class CallFlow {
    private final static Logger LOGGER = LoggerFactory.getLogger(CallFlow.class);

    private final Disassembler disassembler;
    private final NavigableMap<Integer, Integer> flowGraph = new TreeMap<>();
    private int longestInstructionSize = 1;

    CallFlow(Disassembler disassembler) {
        this.disassembler = Objects.requireNonNull(disassembler);
    }

    void updateCache(int currentLocation) {
        try {
            int nextPosition = disassembler.getNextInstructionPosition(currentLocation);
            if (nextPosition - currentLocation > longestInstructionSize) {
                longestInstructionSize = nextPosition - currentLocation;
            }
            Integer prev = flowGraph.get(currentLocation);
            if (prev != null) {
                // jump over previous instruction chain until we get to the last one

                int originalPrev = prev;
                while (prev != null && prev < nextPosition) {
                    prev = flowGraph.get(prev);
                }
                if (prev != null && prev != nextPosition) {
                    // If the current instruction points to a different address than before
                    // and the previous instruction chain does not end in the new position, remove the whole chain
                    flowGraph.subMap(originalPrev, true, prev, true).clear();
                }
            }
            flowGraph.put(currentLocation, nextPosition);
        } catch (RuntimeException ex) {
            LOGGER.warn("Could not update call-flow cache", ex);
        }
    }

    /**
     * Will traverse instructions "knownFrom" (inclusive) up to "to" (exclusive).
     * <p>
     * The "knownFrom" has to be <= "to".
     *
     * @param knownFrom start location, inclusive.
     * @param to        stop location, exclusive.
     * @param consumer  action which will be taken for each found instruction, including start location.
     * @return the greatest instruction location (lastKnownFrom) satisfying lastKnownFrom < to
     */
    int traverseUpTo(int knownFrom, int to, Consumer<Integer> consumer) {
        if (knownFrom > to) {
            throw new IllegalArgumentException("from > to!");
        }

        int lastKnownFrom;
        do {
            lastKnownFrom = knownFrom;

            consumer.accept(lastKnownFrom);

            try {
                knownFrom = disassembler.getNextInstructionPosition(knownFrom);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            if (knownFrom - lastKnownFrom > longestInstructionSize) {
                longestInstructionSize = knownFrom - lastKnownFrom;
            }
        } while (knownFrom < to);
        return (knownFrom == to) ? knownFrom : lastKnownFrom;
    }

    void traverseForInstructionCount(int knownFrom, int count, Consumer<Integer> consumer) {
        for (int i = 0; i < count; i++) {
            int lastKnownFrom = knownFrom;

            try {
                knownFrom = disassembler.getNextInstructionPosition(knownFrom);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            if (knownFrom - lastKnownFrom > longestInstructionSize) {
                longestInstructionSize = knownFrom - lastKnownFrom;
            }
            if (lastKnownFrom == knownFrom) {
                break;
            }

            consumer.accept(knownFrom);
        }
    }

    void traverseBackForInstructionCount(int knownFrom, int count, Consumer<Integer> consumer) {
        for (int i = 0; i < count; i++) {
            Integer previousLocation = flowGraph.lowerKey(knownFrom);
            if (previousLocation == null) {
                break;
            }

            int lastKnownFrom = knownFrom;
            try {
                knownFrom = disassembler.getNextInstructionPosition(previousLocation);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            if (knownFrom - lastKnownFrom > longestInstructionSize) {
                longestInstructionSize = knownFrom - lastKnownFrom;
            }

            consumer.accept(knownFrom);
            knownFrom = previousLocation;
        }
    }

    private int findGreatestPreviousLocation(int unknownLocation, SortedMap<Integer, Integer> knownLocations) {
        if (knownLocations.isEmpty() || knownLocations.firstKey() > unknownLocation) {
            Integer previousKnownLocation = flowGraph.lowerKey(unknownLocation);
            if (previousKnownLocation != null) {
                return traverseUpTo(previousKnownLocation, unknownLocation, i -> {
                });
            }
        }
        return knownLocations.isEmpty() ? unknownLocation : knownLocations.firstKey();
    }

    List<Integer> getLocations(int from, int to) {
        if (from > to) {
            throw new IllegalArgumentException("From (" + from + ") > to (" + to + ") !");
        }
        if (from < 0) {
            from = 0;
        }
        if (to < 0) {
            return Collections.emptyList();
        }

        SortedMap<Integer, Integer> knownInterval = flowGraph.subMap(from, true, to, true);
        List<Integer> locations = new ArrayList<>();

        int lastLocation = -1;
        if (!knownInterval.containsKey(from)) {
            from = findGreatestPreviousLocation(from, knownInterval);
            if (!knownInterval.isEmpty() && from < knownInterval.firstKey()) {
                lastLocation = traverseUpTo(from, knownInterval.firstKey(), locations::add);
                if (lastLocation != knownInterval.firstKey()) {
                    lastLocation = disassembler.getNextInstructionPosition(lastLocation);
                }
            }
        }

        // keep locations sorted!
        boolean skipNext = false;
        List<Integer> invalidLocations = new ArrayList<>();
        for (Map.Entry<Integer, Integer> currentLocation : knownInterval.entrySet()) {
            int currentDecodedLocation = currentLocation.getKey();
            if (skipNext) {
                skipNext = false;
                if (lastLocation > currentDecodedLocation) {
                    invalidLocations.add(currentDecodedLocation);
                    continue;
                }
            }

            if (lastLocation != -1 && lastLocation < currentDecodedLocation) {
                lastLocation = traverseUpTo(lastLocation, currentDecodedLocation, locations::add);

                if (lastLocation < currentDecodedLocation) {
                    invalidLocations.add(currentDecodedLocation);
                    // move ahead because we will try to traverse to the end from the lastLocation
                    // after the loop
                    lastLocation = disassembler.getNextInstructionPosition(lastLocation);
                    break;
                } else if (lastLocation == to) {
                    // we fit
                    locations.add(currentDecodedLocation);
                }
            } else {
                if (lastLocation > currentDecodedLocation) {
                    skipNext = true;
                } else {
                    locations.add(currentDecodedLocation);
                    lastLocation = currentLocation.getValue();
                }
            }
        }
        invalidLocations.forEach(flowGraph::remove);

        if (lastLocation == -1) {
            lastLocation = from;
        }

        if (lastLocation < to) {
            int newTo = traverseUpTo(lastLocation, to, locations::add);
            if (newTo == to) {
                locations.add(newTo);
            }
        }

        return locations;
    }

    void flushCache(int fromLocationInclusive, int toLocationExclusive) {
        flowGraph.subMap(fromLocationInclusive, toLocationExclusive).clear();
    }

    int getLongestInstructionSize() {
        return longestInstructionSize;
    }
}
