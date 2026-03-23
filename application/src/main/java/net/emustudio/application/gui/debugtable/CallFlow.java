/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * Caches decoded instruction flow for the debug table.
 * <p>
 * The cache stores, for each known instruction location, the location of the next instruction as reported by the
 * {@link Disassembler}. The resulting graph can contain disconnected segments and cycles. Callers can use the cached
 * edges to traverse forward, walk back through already known locations, or reconstruct instruction start locations for
 * a visible address range.
 */
@ThreadSafe
class CallFlow {
    private final static Logger LOGGER = LoggerFactory.getLogger(CallFlow.class);

    private final Disassembler disassembler;
    private final NavigableMap<Integer, Integer> flowGraph = new TreeMap<>(); // location -> next location
    private int longestInstructionSize = 1;

    /**
     * Creates an empty call-flow cache backed by the supplied disassembler.
     *
     * @param disassembler source of instruction boundaries
     * @throws NullPointerException if {@code disassembler} is {@code null}
     */
    CallFlow(Disassembler disassembler) {
        this.disassembler = Objects.requireNonNull(disassembler);
    }

    /**
     * Refreshes the cached successor for one decoded instruction.
     * <p>
     * If the instruction now points to a different next location than before, any cached linear chain that is no
     * longer reachable from {@code currentLocation} is discarded. This keeps the cache consistent when the underlying
     * code changes, for example after self-modifying code.
     *
     * @param currentLocation instruction location to decode
     */
    synchronized void updateCache(int currentLocation) {
        try {
            int nextPosition = disassembler.getNextInstructionPosition(currentLocation);
            updateLongestInstructionSize(currentLocation, nextPosition);

            Integer prev = flowGraph.get(currentLocation);
            if (prev != null && prev != nextPosition) {
                // jump over previous instruction chain until we get to the last one
                int originalPrev = prev;
                while (prev != null && prev < nextPosition) {
                    prev = flowGraph.get(prev);
                }
                if (prev != null) {
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

    private void updateLongestInstructionSize(int from, int to) {
        int size = to - from;
        if (size > longestInstructionSize) {
            longestInstructionSize = size;
        }
    }

    /**
     * Traverses instruction starts from {@code knownFrom} toward {@code to}.
     * <p>
     * The traversal includes {@code knownFrom} and stops before passing {@code to}, when the disassembler reports the
     * end of available data, or when a zero-length instruction would cause no forward progress.
     *
     * @param knownFrom first instruction location to report, inclusive
     * @param to stop location, exclusive
     * @param consumer action invoked for each visited instruction location, including {@code knownFrom}
     * @return {@code to} when it is reached exactly; otherwise the last visited instruction location below {@code to}
     * @throws IllegalArgumentException if {@code knownFrom > to}
     */
    synchronized int traverseUpTo(int knownFrom, int to, Consumer<Integer> consumer) {
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
            updateLongestInstructionSize(lastKnownFrom, knownFrom);
            if (lastKnownFrom == knownFrom) {
                break;
            }
        } while (knownFrom < to);
        return (knownFrom == to) ? knownFrom : lastKnownFrom;
    }

    /**
     * Traverses forward by at most {@code count} instructions starting after {@code knownFrom}.
     *
     * @param knownFrom already known instruction location used as the starting point
     * @param count maximum number of forward steps to perform
     * @param consumer action invoked for each newly reached instruction location
     */
    synchronized void traverseForInstructionCount(int knownFrom, int count, Consumer<Integer> consumer) {
        for (int i = 0; i < count; i++) {
            int lastKnownFrom = knownFrom;

            try {
                knownFrom = disassembler.getNextInstructionPosition(knownFrom);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            updateLongestInstructionSize(lastKnownFrom, knownFrom);
            if (lastKnownFrom == knownFrom) {
                break;
            }
            consumer.accept(knownFrom);
        }
    }

    /**
     * Traverses backward through cached instruction locations.
     * <p>
     * Only locations already present in the cache can be visited. For each step, the closest cached location lower
     * than the current one is emitted.
     *
     * @param knownFrom instruction location from which to start looking backward
     * @param count maximum number of cached predecessors to visit
     * @param consumer action invoked for each visited predecessor location
     */
    synchronized void traverseBackForInstructionCount(int knownFrom, int count, Consumer<Integer> consumer) {
        for (int i = 0; i < count; i++) {
            Integer previousLocation = flowGraph.lowerKey(knownFrom);
            if (previousLocation == null) {
                break;
            }

            int nextOfPrevious;
            try {
                nextOfPrevious = disassembler.getNextInstructionPosition(previousLocation);
            } catch (IndexOutOfBoundsException e) {
                break;
            }
            updateLongestInstructionSize(previousLocation, nextOfPrevious);
            consumer.accept(previousLocation);
            knownFrom = previousLocation;
        }
    }

    /**
     * Finds the best location from which to resume decoding before an interval with missing cache entries.
     *
     * @param unknownLocation first location whose predecessor is not yet known
     * @param knownLocations cached locations already present in the requested interval
     * @return the earliest location from which the interval can be reconstructed
     */
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

    /**
     * Returns instruction start locations relevant to the inclusive address interval {@code [from, to]}.
     * <p>
     * The result is sorted in ascending order. When the interval starts in the middle of a known instruction, the
     * returned list can include the closest preceding instruction start needed to cover that address. Negative
     * {@code from} values are clamped to zero; a negative {@code to} yields an empty list.
     *
     * @param from lower bound of the requested address interval, inclusive
     * @param to upper bound of the requested address interval, inclusive
     * @return sorted instruction start locations covering the requested interval as far as decoding allows
     * @throws IllegalArgumentException if {@code from > to}
     */
    synchronized List<Integer> getLocations(int from, int to) {
        if (from > to) {
            throw new IllegalArgumentException("From (" + from + ") > to (" + to + ") !");
        }
        from = Math.max(0, from);
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

    /**
     * Removes cached instruction-flow edges whose start location lies in the specified half-open interval.
     *
     * @param fromLocationInclusive lower bound of the cache entries to remove, inclusive
     * @param toLocationExclusive upper bound of the cache entries to remove, exclusive
     */
    synchronized void flushCache(int fromLocationInclusive, int toLocationExclusive) {
        flowGraph.subMap(fromLocationInclusive, toLocationExclusive).clear();
    }

    /**
     * Returns the largest instruction size seen while decoding so far.
     *
     * @return maximum observed difference between an instruction location and its successor
     */
    int getLongestInstructionSize() {
        return longestInstructionSize;
    }
}
