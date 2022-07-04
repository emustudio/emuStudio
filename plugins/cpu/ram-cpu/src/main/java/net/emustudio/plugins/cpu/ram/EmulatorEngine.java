/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.cpu.ram;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import net.emustudio.plugins.memory.ram.api.RAMInstruction;
import net.emustudio.plugins.memory.ram.api.RAMMemoryContext;
import net.emustudio.plugins.memory.ram.api.RAMValue;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class EmulatorEngine {
    private final AbstractTapeContext inputTape;
    private final AbstractTapeContext outputTape;
    private final AbstractTapeContext storageTape;

    private final RAMMemoryContext memory;
    public final AtomicInteger IP = new AtomicInteger();

    public EmulatorEngine(AbstractTapeContext inputTape, AbstractTapeContext outputTape,
                          AbstractTapeContext storageTape, RAMMemoryContext memory) {
        this.inputTape = Objects.requireNonNull(inputTape);
        this.outputTape = Objects.requireNonNull(outputTape);
        this.storageTape = Objects.requireNonNull(storageTape);
        this.memory = Objects.requireNonNull(memory);
    }

    public boolean setInstructionLocation(int location) {
        if (location < 0) {
            return false;
        }
        IP.set(location);
        return true;
    }

    public void reset(int location) {
        IP.set(location);
        int position = 0;
        inputTape.clear();
        for (RAMValue input : memory.getInputs()) {
            inputTape.setSymbolAt(position++, toSymbol(input));
        }
        storageTape.clear();
        outputTape.clear();
    }

    public CPU.RunState step() throws IOException {
        RAMInstruction instr = memory.read(IP.getAndIncrement());
        if (instr == null) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }

        switch (instr.getOpcode()) {
            case READ:
                TapeSymbol input = inputTape.readData();
                inputTape.moveRight();
                getRegister(instr).ifPresent(r -> storageTape.setSymbolAt(r, input));
                break;
            case WRITE:
                getValue(instr).ifPresent(outputTape::writeData);
                outputTape.moveRight();
                break;
            case LOAD:
                getValue(instr).ifPresent(s -> storageTape.setSymbolAt(0, s));
                break;
            case STORE:
                getRegister(instr)
                    .ifPresent(o -> storageTape.getSymbolAt(0).ifPresent(r -> storageTape.setSymbolAt(o, r)));
                break;
            case ADD:
                getValue(instr).ifPresent(op -> arithmetic(op, Integer::sum));
                break;
            case SUB:
                getValue(instr).ifPresent(op -> arithmetic(op, (a, b) -> a - b));
                break;
            case MUL:
                getValue(instr).ifPresent(op -> arithmetic(op, (a, b) -> a * b));
                break;
            case DIV:
                getValue(instr).ifPresent(op -> arithmetic(op, (a, b) -> a / b));
                break;
            case JMP:
                instr
                    .getLabel()
                    .ifPresentOrElse(o -> IP.set(o.getAddress()), () -> {
                        throw new RuntimeException("Instruction operand contains non-numeric value: " + instr);
                    });
                break;
            case JZ: {
                int r0 = getR0();
                if (r0 == 0) {
                    instr
                        .getLabel()
                        .ifPresentOrElse(o -> IP.set(o.getAddress()), () -> {
                            throw new RuntimeException("Instruction operand contains non-numeric value: " + instr);
                        });
                }
                break;
            }
            case JGTZ: {
                int r0 = getR0();
                if (r0 > 0) {
                    instr
                        .getLabel()
                        .ifPresentOrElse(o -> IP.set(o.getAddress()), () -> {
                            throw new RuntimeException("Instruction operand contains non-numeric value: " + instr);
                        });
                }
                break;
            }
            case HALT:
                return CPU.RunState.STATE_STOPPED_NORMAL;
            default:
                return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private int getR0() {
        return storageTape
            .getSymbolAt(0)
            .filter(r0 -> r0 != TapeSymbol.EMPTY)
            .map(r0 -> {
                if (r0.type != TapeSymbol.Type.NUMBER) {
                    throw new RuntimeException("Register 0 contains non-numeric value: " + r0);
                }
                return r0.number;
            }).orElse(0);
    }

    private void arithmetic(TapeSymbol operand, BiFunction<Integer, Integer, Integer> operation) {
        if (operand.type != TapeSymbol.Type.NUMBER) {
            throw new RuntimeException("Operand is non-numeric: " + operand);
        }
        int r0 = getR0();
        storageTape.setSymbolAt(0, new TapeSymbol(operation.apply(r0, operand.number)));
    }

    private Optional<TapeSymbol> getValue(RAMInstruction instruction) {
        switch (instruction.getDirection()) {
            case CONSTANT:
                return instruction.getOperand().map(this::toSymbol);
            case DIRECT:
            case INDIRECT:
                return getRegister(instruction).flatMap(storageTape::getSymbolAt);
        }
        throw new IllegalStateException("Unexpected direction: " + instruction.getDirection());
    }

    private Optional<Integer> getRegister(RAMInstruction instruction) {
        switch (instruction.getDirection()) {
            case DIRECT:
                return instruction.getOperand().map(r -> {
                    if (r.getType() != RAMValue.Type.NUMBER) {
                        throw new RuntimeException("Instruction has non-numeric operand: " + instruction);
                    }
                    return r.getNumberValue();
                });
            case INDIRECT:
                return instruction.getOperand().flatMap(r -> {
                    if (r.getType() != RAMValue.Type.NUMBER) {
                        throw new RuntimeException("Instruction has non-numeric operand: " + instruction);
                    }
                    return storageTape
                        .getSymbolAt(r.getNumberValue())
                        .map(rr -> {
                            if (rr.type != TapeSymbol.Type.NUMBER) {
                                throw new RuntimeException("Value of register " + rr + " is non-numeric");
                            }
                            return rr.number;
                        });
                });
        }
        throw new IllegalStateException("Unexpected direction: " + instruction.getDirection());
    }

    private TapeSymbol toSymbol(RAMValue value) {
        return (value.getType() == RAMValue.Type.NUMBER) ?
            new TapeSymbol(value.getNumberValue()) : new TapeSymbol(value.getStringValue());
    }
}
