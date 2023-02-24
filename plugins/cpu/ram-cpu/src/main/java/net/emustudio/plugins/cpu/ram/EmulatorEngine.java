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
package net.emustudio.plugins.cpu.ram;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import net.emustudio.plugins.memory.ram.api.RamValue;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class EmulatorEngine {
    public final AtomicInteger IP = new AtomicInteger();
    private final AbstractTapeContext inputTape;
    private final AbstractTapeContext outputTape;
    private final AbstractTapeContext storageTape;
    private final RamMemoryContext memory;

    public EmulatorEngine(AbstractTapeContext inputTape, AbstractTapeContext outputTape,
                          AbstractTapeContext storageTape, RamMemoryContext memory) {
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
        for (RamValue input : memory.getInputs()) {
            inputTape.setSymbolAt(position++, toSymbol(input));
        }
        storageTape.clear();
        outputTape.clear();
    }

    public CPU.RunState step() {
        RamInstruction instruction = memory.read(IP.getAndIncrement());
        if (instruction == null) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }

        switch (instruction.getOpcode()) {
            case READ:
                TapeSymbol input = inputTape.readData();
                inputTape.moveRight();
                getRegisterNumber(instruction).ifPresent(r -> storageTape.setSymbolAt(r, input));
                break;
            case WRITE:
                evaluateOperand(instruction).ifPresent(outputTape::writeData);
                outputTape.moveRight();
                break;
            case LOAD:
                evaluateOperand(instruction).ifPresent(v -> storageTape.setSymbolAt(0, v));
                break;
            case STORE:
                TapeSymbol r0 = storageTape.getSymbolAt(0).orElse(TapeSymbol.EMPTY);
                getRegisterNumber(instruction).ifPresent(r -> storageTape.setSymbolAt(r, r0));
                break;
            case ADD:
                evaluateOperand(instruction).ifPresent(v -> arithmetic(v, Integer::sum));
                break;
            case SUB:
                evaluateOperand(instruction).ifPresent(v -> arithmetic(v, (a, b) -> a - b));
                break;
            case MUL:
                evaluateOperand(instruction).ifPresent(v -> arithmetic(v, (a, b) -> a * b));
                break;
            case DIV:
                evaluateOperand(instruction).ifPresent(v -> arithmetic(v, (a, b) -> a / b));
                break;
            case JMP:
                instruction
                        .getLabel()
                        .ifPresentOrElse(o -> IP.set(o.getAddress()), () -> {
                            throw new RuntimeException("Instruction operand contains non-numeric value: " + instruction);
                        });
                break;
            case JZ: {
                if (isEmpty(storageTape.getSymbolAt(0).orElse(TapeSymbol.EMPTY))) {
                    instruction
                            .getLabel()
                            .ifPresentOrElse(o -> IP.set(o.getAddress()), () -> {
                                throw new RuntimeException("Instruction operand contains non-numeric value: " + instruction);
                            });
                }
                break;
            }
            case JGTZ: {
                if (getR0() > 0) {
                    instruction
                            .getLabel()
                            .ifPresentOrElse(o -> IP.set(o.getAddress()), () -> {
                                throw new RuntimeException("Instruction operand contains non-numeric value: " + instruction);
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
                .orElse(TapeSymbol.EMPTY)
                .number;
    }

    private boolean isEmpty(TapeSymbol s) {
        switch (s.type) {
            case NUMBER:
                return s.number == 0;
            case STRING:
                return s.string == null || s.string.isEmpty();
        }
        throw new RuntimeException("Unexpected symbol type: " + s);
    }

    private void arithmetic(TapeSymbol operand, BiFunction<Integer, Integer, Integer> operation) {
        storageTape.setSymbolAt(0, TapeSymbol.fromInt(operation.apply(getR0(), operand.number)));
    }

    private Optional<Integer> getRegisterNumber(RamInstruction instruction) {
        Optional<RamValue> operand = instruction.getOperand();
        switch (instruction.getDirection()) {
            case CONSTANT:
            case DIRECT:
                return operand.map(RamValue::getNumberValue);
            case INDIRECT:
                return operand.map(RamValue::getNumberValue)
                        .flatMap(storageTape::getSymbolAt)
                        .map(t -> t.number);
        }
        throw new RuntimeException("Unexpected direction: " + instruction.getDirection());
    }

    private Optional<TapeSymbol> evaluateOperand(RamInstruction instruction) {
        Optional<RamValue> operand = instruction.getOperand();
        switch (instruction.getDirection()) {
            case CONSTANT:
                return operand.map(this::toSymbol);
            case DIRECT:
                return operand.map(RamValue::getNumberValue).flatMap(storageTape::getSymbolAt);
            case INDIRECT:
                return operand.map(RamValue::getNumberValue)
                        .flatMap(storageTape::getSymbolAt)
                        .map(t -> t.number)
                        .flatMap(storageTape::getSymbolAt);
        }
        throw new IllegalStateException("Unexpected direction: " + instruction.getDirection());
    }

    private TapeSymbol toSymbol(RamValue value) {
        return (value.getType() == RamValue.Type.NUMBER) ?
                new TapeSymbol(value.getNumberValue()) : new TapeSymbol(value.getStringValue());
    }
}
