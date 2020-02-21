/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.gui.debugTable;

import emulib.plugins.cpu.Disassembler;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static emustudio.gui.debugTable.PaginatingDisassembler.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MockHelper {
    final static int MEMORY_SIZE = 0x10000;
    final static int LONGEST_INSTR = 2;
    final static int CURRENT_INSTR = 50;
    final static int HALF_PAGE_MAX_BYTES = INSTR_PER_HALF_PAGE * LONGEST_INSTR;

    private static CallFlow callFlow;


    static CallFlow makeCallFlow(Disassembler disassembler, int... updateLocations) {
        CallFlow callFlow = new CallFlow(disassembler);
        for (int knownLocation : updateLocations) {
            callFlow.updateCache(knownLocation);
        }
        return callFlow;
    }

    static CallFlow makeCallFlow(int... updateLocations) {
        return makeCallFlow(makeDisassembler(), updateLocations);
    }

    static CallFlow makeCallFlowStep(int... updateLocations) {
        return makeCallFlow(makeDisassemblerStep(), updateLocations);
    }

    static DisassemblerStub makeDisassembler() {
        return new DisassemblerStub(10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    private static DisassemblerStub makeDisassemblerStep() {
        return new DisassemblerStub(10, 2, -1, 4, -1, 6, -1, 8, -1, 10, -1);
    }

    static void modify(CallFlow callFlow, DisassemblerStub disasm, int location, int value) {
        disasm.set(location, value);
        callFlow.updateCache(location);
    }

    static CallFlow mockCallFlow(int from, int to, Integer... interval) {
        return mockCallFlow(from, to, new ArrayList<>(Arrays.asList(interval)));
    }

    static CallFlow mockCallFlow(int from, int to, List<Integer> interval) {
        CallFlow callFlowMock = mock(CallFlow.class);
        when(callFlowMock.getLongestInstructionSize()).thenReturn(LONGEST_INSTR);

        when(callFlowMock.getLocations(from, to)).thenReturn(interval);
        return callFlowMock;
    }

    private static CallFlow mockBasicCallFlow(int longestInstrSize) {
        CallFlow callFlowMock = mock(CallFlow.class);
        when(callFlowMock.getLongestInstructionSize()).thenReturn(longestInstrSize);
        return callFlowMock;
    }

    private static void expectTraverse(int from, int to, int instructionSize) {
        ArgumentCaptor<Consumer> lambdaCaptor = ArgumentCaptor.forClass(Consumer.class);
        when(callFlow.traverseUpTo(eq(from), eq(to), lambdaCaptor.capture())).thenAnswer(invocationOnMock -> {
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
        callFlow = mockBasicCallFlow(instructionsSize);
        List<Integer> currentInstructions = new ArrayList<>();


        if (maxKnownPage == 0) {
            when(callFlow.getLocations(pageMin, pageCurr))
                .thenReturn(instructions(instructionsSize, INSTR_PER_HALF_PAGE + 1, pageMin));
            when(callFlow.getLocations(pageCurr, pageMax))
                .thenReturn(instructions(instructionsSize, INSTR_PER_HALF_PAGE + 1, pageCurr));
        }

        while (maxKnownPage != 0) {
            when(callFlow.getLocations(pageMin, pageCurr))
                .thenReturn(instructions(instructionsSize, INSTR_PER_HALF_PAGE + 1, pageMin));
            when(callFlow.getLocations(pageCurr, pageMax))
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

            when(callFlow.getLocations(nextPageMin, nextPageCurr))
                .thenReturn(instructions(instructionsSize, INSTR_PER_HALF_PAGE + 1, nextPageMin));
            if (nextPageCurr > pageCurr) {
                expectTraverse(pageCurr, nextPageCurr, instructionsSize);
            } else if (nextPageCurr < pageCurr) {
                expectTraverse(nextPageCurr, pageCurr, instructionsSize);
            }
            when(callFlow.getLocations(nextPageCurr, nextPageCurr + instructionsSize * INSTR_PER_HALF_PAGE))
                .thenReturn(instructions(instructionsSize, INSTR_PER_HALF_PAGE + 1, nextPageCurr));

            pageMin = nextPageMin;
            pageCurr = nextPageCurr;
            pageMax = nextPageMax;

            maxKnownPage += increment;
            currentInstructions.add(pageCurr);
        }

        PaginatingDisassembler asm = new PaginatingDisassembler(callFlow, MEMORY_SIZE);

        currentInstructions.forEach(location -> {
            asm.rowToLocation(currentLocation, 0);
            asm.rowToLocation(currentLocation, CURRENT_INSTR_ROW);
            asm.rowToLocation(currentLocation, INSTR_PER_PAGE - 1);

            if (next) {
                asm.pageNext();
            } else {
                asm.pagePrevious();
            }
        });

        return asm;
    }

    static DisassemblerStub makeDisassembler(int memorySize, int step) {
        int[] nextPositions = new int[memorySize];
        Arrays.fill(nextPositions, -1);

        for (int i = step; i <= memorySize; i += step) {
            nextPositions[i - step] = i;
        }

        return new DisassemblerStub(memorySize, nextPositions);
    }

}
