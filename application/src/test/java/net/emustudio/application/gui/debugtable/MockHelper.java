/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugtable;

import net.emustudio.application.gui.debugtable.PaginatingDisassembler.BoundaryDecoder;

import net.emustudio.emulib.plugin.cpu.Disassembler;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static net.emustudio.application.gui.debugtable.PaginatingDisassembler.INSTR_PER_PAGE;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MockHelper {
    final static int MEMORY_SIZE = 0x10000;
    final static int LONGEST_INSTR = 2;
    final static int CURRENT_INSTR = 50;
    final static int INSTR_PER_HALF_PAGE = INSTR_PER_PAGE / 2;
    final static int HALF_PAGE_MAX_BYTES = INSTR_PER_HALF_PAGE * LONGEST_INSTR;

    private static BoundaryDecoder boundaryDecoder;


    static BoundaryDecoder makeBoundaryDecoder(Disassembler disassembler, int... updateLocations) {
        BoundaryDecoder boundaryDecoder = new BoundaryDecoder(disassembler);
        for (int knownLocation : updateLocations) {
            boundaryDecoder.updateCache(knownLocation);
        }
        return boundaryDecoder;
    }

    static BoundaryDecoder makeBoundaryDecoder(int... updateLocations) {
        return makeBoundaryDecoder(makeDisassembler(defaultDisassembly()), updateLocations);
    }

    static BoundaryDecoder makeBoundaryDecoderStep(int... updateLocations) {
        return makeBoundaryDecoder(makeDisassembler(defaultStepDisassembly()), updateLocations);
    }

    static int[] defaultDisassembly() {
        return new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    }

    static int[] defaultStepDisassembly() {
        return new int[] {2, -1, 4, -1, 6, -1, 8, -1, 10, -1};
    }

    static void modify(BoundaryDecoder boundaryDecoder, int[] nextPositions, int location, int value) {
        nextPositions[location] = value;
        boundaryDecoder.updateCache(location);
    }

    static BoundaryDecoder mockBoundaryDecoder(int from, int to, Integer... interval) {
        return mockBoundaryDecoder(from, to, new ArrayList<>(Arrays.asList(interval)));
    }

    static BoundaryDecoder mockBoundaryDecoder(int from, int to, List<Integer> interval) {
        BoundaryDecoder boundaryDecoderMock = mock(BoundaryDecoder.class);
        when(boundaryDecoderMock.getLongestInstructionSize()).thenReturn(LONGEST_INSTR);

        when(boundaryDecoderMock.getLocations(from, to)).thenReturn(interval);
        return boundaryDecoderMock;
    }

    private static BoundaryDecoder mockBasicBoundaryDecoder(int longestInstrSize) {
        BoundaryDecoder boundaryDecoderMock = mock(BoundaryDecoder.class);
        when(boundaryDecoderMock.getLongestInstructionSize()).thenReturn(longestInstrSize);
        return boundaryDecoderMock;
    }

    @SuppressWarnings("unchecked")
    private static void expectTraverse(int from, int to, int instructionSize) {
        ArgumentCaptor<Consumer> lambdaCaptor = ArgumentCaptor.forClass(Consumer.class);
        when(boundaryDecoder.traverseUpTo(eq(from), eq(to), lambdaCaptor.capture())).thenAnswer(invocationOnMock -> {
            for (int i = from; i < to + instructionSize; i += instructionSize) {
                lambdaCaptor.getValue().accept(i);
            }

            return CURRENT_INSTR;
        });
    }

    private static List<Integer> instructions(int size, int count, int from) {
        List<Integer> sequence = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            sequence.add(from + i * size);
        }
        return sequence;
    }


    static PaginatingDisassembler makeDisassemblerWithFixedSizedInstructions(int currentLocation, int maxKnownPage,
                                                                             int instructionsSize, boolean next) {
        final int halfBytes = INSTR_PER_HALF_PAGE * instructionsSize;

        int pageMin = Math.max(0, currentLocation - halfBytes);
        int pageCurr = currentLocation;
        int pageMax = currentLocation + halfBytes;

        int increment = maxKnownPage > 0 ? -1 : 1;
        boundaryDecoder = mockBasicBoundaryDecoder(instructionsSize);
        List<Integer> currentInstructions = new ArrayList<>();


        if (maxKnownPage == 0) {
            when(boundaryDecoder.getLocations(pageMin, pageCurr))
                    .thenReturn(instructions(instructionsSize, INSTR_PER_HALF_PAGE + 1, pageMin));
            when(boundaryDecoder.getLocations(pageCurr, pageMax))
                    .thenReturn(instructions(instructionsSize, INSTR_PER_HALF_PAGE + 1, pageCurr));
        }

        while (maxKnownPage != 0) {
            when(boundaryDecoder.getLocations(pageMin, pageCurr))
                    .thenReturn(instructions(instructionsSize, INSTR_PER_HALF_PAGE + 1, pageMin));
            when(boundaryDecoder.getLocations(pageCurr, pageMax))
                    .thenReturn(instructions(instructionsSize, INSTR_PER_HALF_PAGE + 1, pageCurr));

            int nextPageMin = pageMax;
            int nextPageCurr = pageCurr + instructionsSize * (INSTR_PER_PAGE - 1);
            int nextPageMax = nextPageCurr + halfBytes;

            if (!next) {
                nextPageCurr = Math.max(0, pageCurr - instructionsSize * (INSTR_PER_PAGE - 1));
                nextPageMin = Math.max(0, nextPageCurr - halfBytes - instructionsSize);
                nextPageMax = pageMin;

                if (nextPageMin == 0) {
                    maxKnownPage = -increment;
                }
            }

            when(boundaryDecoder.getLocations(nextPageMin, nextPageCurr))
                    .thenReturn(instructions(instructionsSize, INSTR_PER_HALF_PAGE + 1, nextPageMin));
            if (nextPageCurr > pageCurr) {
                expectTraverse(pageCurr, nextPageCurr, instructionsSize);
            } else if (nextPageCurr < pageCurr) {
                expectTraverse(nextPageCurr, pageCurr, instructionsSize);
            }
            when(boundaryDecoder.getLocations(nextPageCurr, nextPageCurr + instructionsSize * INSTR_PER_HALF_PAGE))
                    .thenReturn(instructions(instructionsSize, INSTR_PER_HALF_PAGE + 1, nextPageCurr));

            pageMin = nextPageMin;
            pageCurr = nextPageCurr;
            pageMax = nextPageMax;

            maxKnownPage += increment;
            currentInstructions.add(pageCurr);
        }

        PaginatingDisassembler asm = new PaginatingDisassembler(boundaryDecoder, MEMORY_SIZE);

        currentInstructions.forEach(location -> {
            asm.rowToLocation(currentLocation, 0);
            asm.rowToLocation(currentLocation, INSTR_PER_HALF_PAGE);
            asm.rowToLocation(currentLocation, INSTR_PER_PAGE - 1);

            if (next) {
                asm.pageNext();
            } else {
                asm.pagePrevious();
            }
        });

        return asm;
    }

    static Disassembler makeDisassembler(int memorySize, int step) {
        int[] nextPositions = new int[memorySize];
        Arrays.fill(nextPositions, -1);

        for (int i = step; i <= memorySize; i += step) {
            nextPositions[i - step] = i;
        }

        return makeDisassembler(nextPositions);
    }

    static Disassembler makeDisassembler(int[] nextPositions) {
        Disassembler disassembler = mock(Disassembler.class);
        when(disassembler.getNextInstructionPosition(anyInt())).thenAnswer(invocation -> {
            int position = invocation.getArgument(0);
            int nextPosition = nextPositions[position];
            if (nextPosition == -1) {
                throw new IndexOutOfBoundsException();
            }
            return nextPosition;
        });
        return disassembler;
    }

    static Disassembler makeDisassembler(int memorySize, int... nextPositions) {
        if (memorySize < nextPositions.length) {
            throw new IllegalArgumentException("Memory size < instruction.length");
        }
        int[] copy = new int[memorySize];
        System.arraycopy(nextPositions, 0, copy, 0, nextPositions.length);
        return makeDisassembler(copy);
    }
}
