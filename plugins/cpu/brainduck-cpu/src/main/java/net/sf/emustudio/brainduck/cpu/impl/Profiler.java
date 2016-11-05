package net.sf.emustudio.brainduck.cpu.impl;

import emulib.plugins.memory.MemoryContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_COPY_DEC_BACKWARD_AND_CLEAR;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_COPY_DEC_FORWARD_AND_CLEAR;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_COPY_INC_BACKWARD_AND_CLEAR;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_COPY_INC_FORWARD_AND_CLEAR;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_DEC;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_DECV;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_INC;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_INCV;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_LOOP_END;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_LOOP_START;
import static net.sf.emustudio.brainduck.cpu.impl.EmulatorEngine.I_STOP;

public class Profiler {
    private final MemoryContext<Short> memory;
    private final Map<Integer, CachedOperation> operationsCache = new HashMap<>();
    private final Map<Integer, Integer> loopEndsCache = new HashMap<>();


    public static final class CachedOperation {
        public int argument;
        public int nextIP;
        public short operation;

        @Override
        public String toString() {
            return "CachedOperation{" +
                "argument=" + argument +
                ", nextIP=" + nextIP +
                ", operation=" + operation +
                '}';
        }
    }

    public Profiler(MemoryContext<Short> memory) {
        this.memory = Objects.requireNonNull(memory);
    }

    public void reset() {
        operationsCache.clear();
    }

    public void optimizeRepeatingOperations(int programSize) {
        int lastOperation = -1;
        short OP;
        for (int tmpIP = 0; tmpIP < programSize; tmpIP++) {
            OP = memory.read(tmpIP);
            if (OP != I_LOOP_START && OP != I_LOOP_END && (lastOperation == OP)) {
                int previousIP = tmpIP - 1;
                CachedOperation operation = new CachedOperation();

                operation.operation = OP;
                operation.argument = 2;

                while ((tmpIP+1) < programSize && (memory.read(tmpIP+1) == lastOperation)) {
                    operation.argument++;
                    tmpIP++;
                }
                operation.nextIP = tmpIP + 1;
                operationsCache.put(previousIP, operation);
            }
            lastOperation = OP;
        }
    }

    public void optimizeLoops(int programSize) {
        short OP;
        for (int tmpIP = 0; tmpIP < programSize; tmpIP++) {
            if (memory.read(tmpIP) != I_LOOP_START) {
                continue;
            }
            int loop_count = 0; // loop nesting level counter

            // we start to look for "]" instruction
            // on the same nesting level (according to loop_count value)
            // IP is pointing at following instruction
            int tmpIP2 = tmpIP + 1;
            while ((tmpIP2 < programSize) && (OP = memory.read(tmpIP2++)) != I_STOP) {
                if (OP == I_LOOP_START) {
                    loop_count++;
                }
                if (OP == I_LOOP_END) {
                    if (loop_count == 0) {
                        loopEndsCache.put(tmpIP, tmpIP2);
                        break;
                    } else {
                        loop_count--;
                    }
                }
            }
        }
    }

    public void optimizeCopyLoops(int programSize) {
        short OP;
        for (int tmpIP = 0; tmpIP < programSize; tmpIP++) {
            OP = memory.read(tmpIP);
            if (OP == I_LOOP_START && tmpIP+2 < programSize) {
                tmpIP++;
                OP = memory.read(tmpIP);
                if (OP == I_DECV && (memory.read(tmpIP + 1) == I_LOOP_END)) {
                    // got [-]
                    operationsCache.put(tmpIP - 1, makeOP_CLEAR(tmpIP));
                    continue;
                }
                CachedOperation copyLoop = findCopyLoop(
                    tmpIP, OP, programSize, I_INC, I_INCV, I_DEC, I_COPY_INC_FORWARD_AND_CLEAR
                );
                if (copyLoop == null) {
                    copyLoop = findCopyLoop(
                        tmpIP, OP, programSize, I_INC, I_DECV, I_DEC, I_COPY_DEC_FORWARD_AND_CLEAR
                    );
                    if (copyLoop == null) {
                        copyLoop = findCopyLoop(
                            tmpIP, OP, programSize, I_DEC, I_INCV, I_INC, I_COPY_INC_BACKWARD_AND_CLEAR
                        );
                        if (copyLoop == null) {
                            copyLoop = findCopyLoop(
                                tmpIP, OP, programSize, I_DEC, I_DECV, I_INC, I_COPY_DEC_BACKWARD_AND_CLEAR
                            );
                        }
                    }
                }

                if (copyLoop != null) {
                    operationsCache.put(tmpIP - 1, copyLoop);
                    tmpIP = copyLoop.nextIP;
                    continue;
                }
            }
        }
    }

    private CachedOperation makeOP_CLEAR(int tmpIP) {
        CachedOperation operation = new CachedOperation();

        operation.operation = 0xA1;
        operation.nextIP = tmpIP + 2;
        return operation;
    }

    private CachedOperation findCopyLoop(int tmpIP, int OP, int programSize, int pointerForwardOP,
                                         int valueOP, int pointerBackwardOP, short resultOP) {
        int expectedOP = pointerForwardOP;
        int anotherIP = tmpIP;
        int anotherOP = OP;
        boolean wasDECV = false;

        if (anotherOP == I_DECV) {
            wasDECV = true;
            anotherIP++;
            if (anotherIP < programSize) {
                anotherOP = memory.read(anotherIP);
            } else {
                return null;
            }
        }

        CachedOperation operation = new CachedOperation();
        while (anotherOP == expectedOP && (anotherIP + 1 < programSize)) {
            if (expectedOP == pointerForwardOP) {
                expectedOP = valueOP;
            } else {
                operation.argument++; // how many pointers beyond should be incremented
                expectedOP = pointerForwardOP;
            }
            anotherIP++;
            anotherOP = memory.read(anotherIP);
        }

        int pointerReturnsLeft = operation.argument;
        while (pointerReturnsLeft > 0 && (anotherOP == pointerBackwardOP) && (anotherIP + 1 < programSize)) {
            pointerReturnsLeft--;
            anotherIP++;
            anotherOP = memory.read(anotherIP);
        }
        if (pointerReturnsLeft == 0 && (wasDECV || anotherOP == I_DECV)) {
            if (!wasDECV && (anotherIP+1) < programSize) {
                anotherIP++;
                anotherOP = memory.read(anotherIP);
            }
            if (anotherOP == I_LOOP_END) {
                operation.operation = resultOP;
                operation.nextIP = anotherIP + 1;

                return operation;
            }
        }
        return null;
    }

    public CachedOperation findCachedOperation(int address) {
        return operationsCache.get(address);
    }

    public Integer findLoopEnd(int address) {
        return loopEndsCache.get(address);
    }
}
