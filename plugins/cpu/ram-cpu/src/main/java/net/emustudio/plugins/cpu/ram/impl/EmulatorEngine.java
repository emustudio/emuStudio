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
package net.emustudio.plugins.cpu.ram.impl;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.plugins.devices.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.cpu.ram.RAMContext;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;

public class EmulatorEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmulatorEngine.class);

    private enum ArithOp {ADD, SUB, MUL, DIV}

    private final static Map<ArithOp, IntBinaryOperator> INT_ARITH_OPS = new HashMap<>();
    private final static Map<ArithOp, DoubleBinaryOperator> DBL_ARITH_OPS = new HashMap<>();

    private final RAMContext context;
    private final RAMMemoryContext memory;
    public int IP;

    static {
        INT_ARITH_OPS.put(ArithOp.ADD, Integer::sum);
        INT_ARITH_OPS.put(ArithOp.SUB, (a, b) -> a - b);
        INT_ARITH_OPS.put(ArithOp.MUL, (a, b) -> a * b);
        INT_ARITH_OPS.put(ArithOp.DIV, (a, b) -> a / b);

        DBL_ARITH_OPS.put(ArithOp.ADD, Double::sum);
        DBL_ARITH_OPS.put(ArithOp.SUB, (a, b) -> a - b);
        DBL_ARITH_OPS.put(ArithOp.MUL, (a, b) -> a * b);
        DBL_ARITH_OPS.put(ArithOp.DIV, (a, b) -> a / b);
    }

    public EmulatorEngine(RAMContext context, RAMMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
        this.context = Objects.requireNonNull(context);
    }

    public boolean setInstructionLocation(int pos) {
        if (pos < 0) {
            return false;
        }
        IP = pos;
        return true;
    }

    public void reset(int pos) {
        IP = pos;
        loadInput(context.getInput());
        context.getStorage().clear();
        context.getOutput().clear();
    }

    // called from RAMContext after Input tape attachement
    public void loadInput(AbstractTapeContext tape) {
        tape.clear();
        List<String> data = memory.getInputs();
        if (data == null) {
            return;
        }

        int j = data.size();
        for (int i = 0; i < j; i++) {
            tape.setSymbolAt(i, data.get(i));
        }
    }

    private int getIntegerOperand(RAMInstruction instruction) throws IOException {
        Integer operand = (Integer) instruction.getOperand();
        switch (instruction.getDirection()) {
            case REGISTER:
                return operand;
            case INDIRECT:
                try {
                    operand = Integer.decode(context.getStorage().getSymbolAt(operand));
                    if (operand < 0) {
                        throw new IOException("[" + instruction + "] Indirect operand must be > 0");
                    }
                    return operand;
                } catch (NumberFormatException e) {
                    throw new IOException("[" + instruction + "] Could not parse number", e);
                }
        }
        throw new IOException("[" + instruction + "] Instruction cannot have DIRECT operand!");
    }

    private String arithmetic(String sym0, String sym1, ArithOp op) throws IOException {
        try {
            return String.valueOf(
                INT_ARITH_OPS.get(op).applyAsInt(Integer.decode(sym0), Integer.decode(sym1))
            );
        } catch (NumberFormatException e) {
            try {
                return String.valueOf(
                    DBL_ARITH_OPS.get(op).applyAsDouble(Double.parseDouble(sym0), Double.parseDouble(sym1))
                );
            } catch (NumberFormatException x) {
                throw new IOException("Could not parse numbers " + sym0 + " and/or " + sym1, e);
            }
        }
    }

    private Number decode(String num) {
        if (num == null || num.isEmpty()) {
            return 0;
        }
        try {
            return Integer.decode(num);
        } catch (NumberFormatException e) {
            return Double.parseDouble(num);
        }
    }

    CPU.RunState step() throws IOException {
        AbstractTapeContext storage = context.getStorage();

        RAMInstruction in = memory.read(IP++);
        if (in == null) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }

        int operand;
        String sym0, sym1;

        switch (in.getCode()) {
            case RAMInstruction.READ:
                operand = getIntegerOperand(in);
                String input = context.getInput().readData();
                context.getInput().moveRight();
                storage.setSymbolAt(operand, input);
                break;
            case RAMInstruction.WRITE:
                if (in.getDirection() == RAMInstruction.Direction.DIRECT) {
                    context.getOutput().writeData(String.valueOf(in.getOperand()));
                } else {
                    operand = getIntegerOperand(in);
                    context.getOutput().writeData(storage.getSymbolAt(operand));
                }
                context.getOutput().moveRight();
                break;
            case RAMInstruction.LOAD:
                if (in.getDirection() == RAMInstruction.Direction.DIRECT) {
                    storage.setSymbolAt(0, String.valueOf(in.getOperand()));
                } else {
                    operand = getIntegerOperand(in);
                    storage.setSymbolAt(0, storage.getSymbolAt(operand));
                }
                break;
            case RAMInstruction.STORE:
                operand = getIntegerOperand(in);
                storage.setSymbolAt(operand, storage.getSymbolAt(0));
                break;
            case RAMInstruction.ADD:
                sym0 = storage.getSymbolAt(0);

                if (in.getDirection() == RAMInstruction.Direction.DIRECT) {
                    sym1 = String.valueOf(in.getOperand());
                } else {
                    sym1 = storage.getSymbolAt(getIntegerOperand(in));
                }
                storage.setSymbolAt(0, arithmetic(sym0, sym1, ArithOp.ADD));
                break;
            case RAMInstruction.SUB:
                sym0 = storage.getSymbolAt(0);

                if (in.getDirection() == RAMInstruction.Direction.DIRECT) {
                    sym1 = String.valueOf(in.getOperand());
                } else {
                    sym1 = storage.getSymbolAt(getIntegerOperand(in));
                }
                storage.setSymbolAt(0, arithmetic(sym0, sym1, ArithOp.SUB));
                break;
            case RAMInstruction.MUL:
                sym0 = storage.getSymbolAt(0);

                if (in.getDirection() == RAMInstruction.Direction.DIRECT) {
                    sym1 = String.valueOf(in.getOperand());
                } else {
                    sym1 = storage.getSymbolAt(getIntegerOperand(in));
                }
                storage.setSymbolAt(0, arithmetic(sym0, sym1, ArithOp.MUL));
                break;
            case RAMInstruction.DIV:
                sym0 = storage.getSymbolAt(0);

                if (in.getDirection() == RAMInstruction.Direction.DIRECT) {
                    sym1 = String.valueOf(in.getOperand());
                } else {
                    sym1 = storage.getSymbolAt(getIntegerOperand(in));
                }
                storage.setSymbolAt(0, arithmetic(sym0, sym1, ArithOp.DIV));
                break;
            case RAMInstruction.JMP:
                IP = (Integer) in.getOperand();
                break;
            case RAMInstruction.JZ: {
                String r0 = storage.getSymbolAt(0);
                try {
                    if (decode(r0).intValue() == 0) {
                        IP = (Integer) in.getOperand();
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("[pos={}, JZ {}] Cannot parse operand (expected integer)", IP, r0);
                    return CPU.RunState.STATE_STOPPED_BAD_INSTR;
                }
                break;
            }
            case RAMInstruction.JGTZ:
                String r0 = storage.getSymbolAt(0);
                try {
                    if (decode(r0).intValue() > 0) {
                        IP = (Integer) in.getOperand();
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("[pos={}, JGTZ {}] Cannot parse operand (expected integer)", IP, r0);
                    return CPU.RunState.STATE_STOPPED_BAD_INSTR;
                }
                break;
            case RAMInstruction.HALT:
                return CPU.RunState.STATE_STOPPED_NORMAL;
            default:
                return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        return CPU.RunState.STATE_STOPPED_BREAK;
    }
}
