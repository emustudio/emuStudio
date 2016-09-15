/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.ram.cpu.impl;

import emulib.plugins.cpu.CPU;
import net.sf.emustudio.ram.abstracttape.AbstractTapeContext;
import net.sf.emustudio.ram.memory.RAMInstruction;
import net.sf.emustudio.ram.memory.RAMMemoryContext;
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

    private enum ArithOp { ADD, SUB, MUL, DIV }
    private final static Map<ArithOp, IntBinaryOperator> INT_ARITH_OPS = new HashMap<>();
    private final static Map<ArithOp, DoubleBinaryOperator> DBL_ARITH_OPS = new HashMap<>();

    private final RAMContext context;
    private final RAMMemoryContext memory;
    public int IP;

    static {
        INT_ARITH_OPS.put(ArithOp.ADD, (a,b) -> a+b);
        INT_ARITH_OPS.put(ArithOp.SUB, (a,b) -> a-b);
        INT_ARITH_OPS.put(ArithOp.MUL, (a,b) -> a*b);
        INT_ARITH_OPS.put(ArithOp.DIV, (a,b) -> a/b);

        DBL_ARITH_OPS.put(ArithOp.ADD, (a,b) -> a+b);
        DBL_ARITH_OPS.put(ArithOp.SUB, (a,b) -> a-b);
        DBL_ARITH_OPS.put(ArithOp.MUL, (a,b) -> a*b);
        DBL_ARITH_OPS.put(ArithOp.DIV, (a,b) -> a/b);
    }

    public EmulatorEngine(RAMContext context, RAMMemoryContext memory) {
        this.memory = Objects.requireNonNull(memory);
        this.context = Objects.requireNonNull(context);
    }

    public boolean setInstructionPosition(int pos) {
        if (pos < 0) {
            return false;
        }
        IP = pos;
        return true;
    }

    public void reset(int pos) {
        IP = pos;
        loadInput(context.getInput());
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
        Integer operand = (Integer)instruction.getOperand();
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

    public CPU.RunState step() throws IOException {
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
                String input = context.getInput().read();
                context.getInput().moveRight();
                storage.setSymbolAt(operand, input);
                break;
            case RAMInstruction.WRITE:
                if (in.getDirection() == RAMInstruction.Direction.DIRECT) {
                    context.getOutput().write(String.valueOf(in.getOperand()));
                } else {
                    operand = getIntegerOperand(in);
                    context.getOutput().write(storage.getSymbolAt(operand));
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
                if (r0 == null || r0.equals("")) {
                    IP = (Integer) in.getOperand();
                    break;
                }
                try {
                    int rr0 = Integer.decode(r0);
                    if (rr0 == 0) {
                        IP = (Integer) in.getOperand();
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Cannot parse JZ operand (expected integer)", e);
                    return CPU.RunState.STATE_STOPPED_BAD_INSTR;
                }
                break;
            }
            case RAMInstruction.JGTZ:
                String r0 = storage.getSymbolAt(0);
                if (r0 == null || r0.equals("")) {
                    IP = (Integer) in.getOperand();
                    break;
                }
                try {
                    int rr0 = Integer.decode(r0);
                    if (rr0 > 0) {
                        IP = (Integer) in.getOperand();
                    }
                } catch (NumberFormatException e) {
                    LOGGER.error("Cannot parse JZ operand (expected integer)", e);
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
