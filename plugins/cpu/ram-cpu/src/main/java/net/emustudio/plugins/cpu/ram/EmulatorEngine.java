/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ram;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.plugins.cpu.ram.gui.RamDisassembler;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import net.emustudio.plugins.memory.ram.api.RamInstruction;
import net.emustudio.plugins.memory.ram.api.RamLabel;
import net.emustudio.plugins.memory.ram.api.RamMemoryContext;
import net.emustudio.plugins.memory.ram.api.RamValue;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class EmulatorEngine {
    public final AtomicInteger IP = new AtomicInteger();
    public final MachineDebugging debugger;
    boolean debugStopped;
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
        debugger = new MachineDebugging("RAM", new RamDisassembler(memory));
        debugger.register("IP", 32, () -> Integer.toUnsignedLong(IP.get()), value -> IP.set((int) value));
        debugger.symbol("R0", () -> inspect(storageTape, 0), text -> storageTape.setSymbolAt(0, parseSymbol(text)));
        debugger.symbol("INPUT", () -> inspect(inputTape, inputTape.getHeadPosition()),
                text -> inputTape.setSymbolAt(inputTape.getHeadPosition(), parseSymbol(text)));
        debugger.symbol("OUTPUT", () -> inspect(outputTape, Math.max(0, outputTape.getHeadPosition() - 1)),
                text -> outputTape.setSymbolAt(Math.max(0, outputTape.getHeadPosition() - 1), parseSymbol(text)));
        debugger.inspect("INPUT_POSITION", inputTape::getHeadPosition);
        debugger.inspect("OUTPUT_POSITION", outputTape::getHeadPosition);
        debugger.functions(java.util.Set.of("storage", "input", "output", "read_storage", "write_storage", "read_input", "write_output"), (name, args) -> {
            if (name.startsWith("read_") || name.startsWith("write_")) {
                return debugger.accessed(name.substring(name.indexOf('_') + 1), MachineDebugging.location(args), name.startsWith("write_"));
            }
            AbstractTapeContext tape = name.equals("storage") ? storageTape : name.equals("input") ? inputTape : outputTape;
            return inspect(tape, MachineDebugging.location(args));
        });
    }

    private static TapeSymbol parseSymbol(String text) {
        String source = text.trim();
        if (source.startsWith("\"") || source.startsWith("'")) {
            char quote = source.charAt(0);
            boolean escaped = false;
            for (int i = 1; i < source.length() - 1; i++) {
                char next = source.charAt(i);
                if (!escaped && next == quote) throw new IllegalArgumentException("Expected one string literal");
                if (!escaped && next == '\\') escaped = true;
                else escaped = false;
            }
            String value = net.emustudio.emulib.plugins.debugger.ExpressionParser.parse(source,
                    name -> { throw new IllegalArgumentException("Expected a string literal"); },
                    name -> { throw new IllegalArgumentException("Expected a string literal"); }).display().text;
            return TapeSymbol.fromString(value);
        }
        return TapeSymbol.guess(text);
    }

    private static Object inspect(AbstractTapeContext tape, int at) {
        TapeSymbol value = tape.getSymbolAt(at).orElse(TapeSymbol.EMPTY);
        return value.type == TapeSymbol.Type.NUMBER ? value.number : value.string;
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
        debugStopped = false;
        if (!debugger.begin(IP.get())) {
            debugStopped = true;
            return CPU.RunState.STATE_STOPPED_BREAK;
        }
        boolean completed = false;
        CPU.RunState result = CPU.RunState.STATE_STOPPED_BAD_INSTR;
        try {
            result = stepInstruction();
            completed = result != CPU.RunState.STATE_STOPPED_BAD_INSTR
                    && result != CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
        } finally {
            debugStopped = debugger.end(completed) && completed
                    && result == CPU.RunState.STATE_STOPPED_BREAK;
        }
        return result;
    }

    private Optional<TapeSymbol> readStorage(int at) {
        Optional<TapeSymbol> value = storageTape.getSymbolAt(at);
        value.ifPresent(symbol -> debugger.tapeAccess("storage", at, symbol, symbol, false));
        return value;
    }

    private void writeStorage(int at, TapeSymbol value) {
        TapeSymbol before = debugger.isCapturing() ? storageTape.getSymbolAt(at).orElse(TapeSymbol.EMPTY) : TapeSymbol.EMPTY;
        storageTape.setSymbolAt(at, value);
        debugger.tapeAccess("storage", at, before, value, true);
    }

    private void writeOutput(TapeSymbol value) {
        int at = outputTape.getHeadPosition();
        TapeSymbol before = debugger.isCapturing() ? outputTape.getSymbolAt(at).orElse(TapeSymbol.EMPTY) : TapeSymbol.EMPTY;
        outputTape.writeData(value);
        debugger.tapeAccess("output", at, before, value, true);
    }

    private CPU.RunState stepInstruction() {
        RamInstruction instruction = memory.read(IP.getAndIncrement());
        if (instruction == null) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }

        switch (instruction.getOpcode()) {
            case READ:
                TapeSymbol input = inputTape.readData();
                debugger.tapeAccess("input", inputTape.getHeadPosition(), input, input, false);
                inputTape.moveRight();
                getRegisterNumber(instruction).ifPresent(r -> writeStorage(r, input));
                break;
            case WRITE:
                evaluateOperand(instruction).ifPresent(this::writeOutput);
                outputTape.moveRight();
                break;
            case LOAD:
                evaluateOperand(instruction).ifPresent(v -> writeStorage(0, v));
                break;
            case STORE:
                TapeSymbol r0 = readStorage(0).orElse(TapeSymbol.EMPTY);
                getRegisterNumber(instruction).ifPresent(r -> writeStorage(r, r0));
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
            case JMP: {
                RamLabel label = instruction.getLabel();
                if (label != null) {
                    IP.set(label.getAddress());
                } else {
                    throw new RuntimeException("Instruction operand contains non-numeric value: " + instruction);
                }
                break;
            }
            case JZ: {
                if (isEmpty(readStorage(0).orElse(TapeSymbol.EMPTY))) {
                    RamLabel label = instruction.getLabel();
                    if (label != null) {
                        IP.set(label.getAddress());
                    } else {
                        throw new RuntimeException("Instruction operand contains non-numeric value: " + instruction);
                    }
                }
                break;
            }
            case JGTZ: {
                if (getR0() > 0) {
                    RamLabel label = instruction.getLabel();
                    if (label != null) {
                        IP.set(label.getAddress());
                    } else {
                        throw new RuntimeException("Instruction operand contains non-numeric value: " + instruction);
                    }
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
        return readStorage(0)
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
        writeStorage(0, TapeSymbol.fromInt(operation.apply(getR0(), operand.number)));
    }

    private Optional<Integer> getRegisterNumber(RamInstruction instruction) {
        RamValue operand = instruction.getOperand();
        if (operand == null) return Optional.empty();
        switch (instruction.getDirection()) {
            case CONSTANT:
            case DIRECT:
                return Optional.of(operand.getNumberValue());
            case INDIRECT:
                return readStorage(operand.getNumberValue())
                        .map(t -> t.number);
        }
        throw new RuntimeException("Unexpected direction: " + instruction.getDirection());
    }

    private Optional<TapeSymbol> evaluateOperand(RamInstruction instruction) {
        RamValue operand = instruction.getOperand();
        if (operand == null) return Optional.empty();
        switch (instruction.getDirection()) {
            case CONSTANT:
                return Optional.of(toSymbol(operand));
            case DIRECT:
                return readStorage(operand.getNumberValue());
            case INDIRECT:
                return readStorage(operand.getNumberValue())
                        .map(t -> t.number)
                        .flatMap(this::readStorage);
        }
        throw new IllegalStateException("Unexpected direction: " + instruction.getDirection());
    }

    private TapeSymbol toSymbol(RamValue value) {
        return (value.getType() == RamValue.Type.NUMBER) ?
                new TapeSymbol(value.getNumberValue()) : new TapeSymbol(value.getStringValue());
    }
}
