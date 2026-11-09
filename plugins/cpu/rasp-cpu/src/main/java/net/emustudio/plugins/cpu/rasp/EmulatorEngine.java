/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.plugins.cpu.rasp.gui.RaspDisassembler;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static net.emustudio.plugins.memory.rasp.gui.Disassembler.HALT;

public class EmulatorEngine {
    public final AtomicInteger IP = new AtomicInteger();
    public final MachineDebugging debugger;
    boolean debugStopped;
    private final AbstractTapeContext inputTape;
    private final AbstractTapeContext outputTape;
    private final RaspMemoryContext memory;
    private final Instruction[] dispatcher = new Instruction[]{
            null,
            this::read,
            this::write_c,
            this::write,
            this::load_c,
            this::load,
            this::store,
            this::add_c,
            this::add,
            this::sub_c,
            this::sub,
            this::mul_c,
            this::mul,
            this::div_c,
            this::div,
            this::jmp,
            this::jz,
            this::jgtz,
            this::halt
    };

    public EmulatorEngine(RaspMemoryContext memory, AbstractTapeContext inputTape, AbstractTapeContext outputTape) {
        this.inputTape = Objects.requireNonNull(inputTape);
        this.outputTape = Objects.requireNonNull(outputTape);
        this.memory = Objects.requireNonNull(memory);
        debugger = new MachineDebugging("RASP", new RaspDisassembler(memory));
        debugger.register("IP", 32, () -> Integer.toUnsignedLong(IP.get()), value -> IP.set((int) value));
        debugger.symbol("ACC", () -> memory.peek(0), text -> memory.write(0, parseWord(text)));
        debugger.symbol("INPUT", () -> inspect(inputTape, inputTape.getHeadPosition()),
                text -> inputTape.setSymbolAt(inputTape.getHeadPosition(), TapeSymbol.fromInt(parseWord(text))));
        debugger.symbol("OUTPUT", () -> inspect(outputTape, Math.max(0, outputTape.getHeadPosition() - 1)),
                text -> outputTape.setSymbolAt(Math.max(0, outputTape.getHeadPosition() - 1), TapeSymbol.fromInt(parseWord(text))));
        debugger.inspect("INPUT_POSITION", inputTape::getHeadPosition);
        debugger.inspect("OUTPUT_POSITION", outputTape::getHeadPosition);
        debugger.functions(java.util.Set.of("mem"), (name, args) -> memory.peek(MachineDebugging.location(args)));
    }

    private static int parseWord(String text) {
        long value = Long.decode(text.trim());
        if (value < Integer.MIN_VALUE || value > 0xffffffffL) throw new IllegalArgumentException("Value exceeds a RASP word");
        return (int) value;
    }

    private static Object inspect(AbstractTapeContext tape, int at) {
        TapeSymbol value = tape.getSymbolAt(at).orElse(TapeSymbol.EMPTY);
        return value.type == TapeSymbol.Type.NUMBER ? value.number : value.string;
    }

    protected void reset(int location) {
        IP.set(location);
        int position = 0;
        inputTape.clear();
        for (int input : memory.getInputs()) {
            inputTape.setSymbolAt(position++, new TapeSymbol(input));
        }
        outputTape.clear();
    }

    public CPU.RunState step() throws IOException {
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

    private CPU.RunState stepInstruction() throws IOException {
        int opcode = readWord(IP.getAndIncrement());
        if (!memory.isInstruction(opcode)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        if (opcode > 0 && opcode <= HALT) {
            return dispatcher[opcode].execute();
        }
        return CPU.RunState.STATE_STOPPED_BAD_INSTR;
    }

    public boolean setInstructionLocation(int location) {
        if (location < 0) {
            return false;
        }
        IP.set(location);
        return true;
    }

    private CPU.RunState read() {
        int register = readWord(IP.getAndIncrement());
        int input = inputTape.readData().number;

        inputTape.moveRight();
        writeWord(register, input);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState write_c() {
        int constant = readWord(IP.getAndIncrement());
        outputTape.writeData(new TapeSymbol(constant));
        outputTape.moveRight();
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState write() {
        int register = readWord(IP.getAndIncrement());
        int value = readWord(register);
        outputTape.writeData(new TapeSymbol(value));
        outputTape.moveRight();
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState load_c() {
        int value = readWord(IP.getAndIncrement());
        writeWord(0, value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState load() {
        int register = readWord(IP.getAndIncrement());
        writeWord(0, readWord(register));
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState store() {
        int register = readWord(IP.getAndIncrement());
        writeWord(register, readWord(0));
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState add_c() {
        int value = readWord(IP.getAndIncrement());
        int r0 = readWord(0);
        writeWord(0, r0 + value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState add() {
        int register = readWord(IP.getAndIncrement());
        int value = readWord(register);
        int r0 = readWord(0);
        writeWord(0, r0 + value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState sub_c() {
        int value = readWord(IP.getAndIncrement());
        int r0 = readWord(0);
        writeWord(0, r0 - value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState sub() {
        int register = readWord(IP.getAndIncrement());
        int value = readWord(register);
        int r0 = readWord(0);
        writeWord(0, r0 - value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState mul_c() {
        int value = readWord(IP.getAndIncrement());
        int r0 = readWord(0);
        writeWord(0, r0 * value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState mul() {
        int register = readWord(IP.getAndIncrement());
        int value = readWord(register);
        int r0 = readWord(0);
        writeWord(0, r0 * value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState div_c() {
        int value = readWord(IP.getAndIncrement());
        int r0 = readWord(0);
        if (value == 0) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        writeWord(0, r0 / value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState div() {
        int register = readWord(IP.getAndIncrement());
        int value = readWord(register);
        int r0 = readWord(0);
        if (value == 0) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        writeWord(0, r0 / value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState jmp() {
        IP.set(readWord(IP.get()));
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState jz() {
        int address = readWord(IP.getAndIncrement());
        int r0 = readWord(0);
        if (r0 == 0) {
            IP.set(address);
        }
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState jgtz() {
        int address = readWord(IP.getAndIncrement());
        int r0 = readWord(0);
        if (r0 > 0) {
            IP.set(address);
        }
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState halt() {
        return CPU.RunState.STATE_STOPPED_NORMAL;
    }

    private int readWord(int address) {
        int value = memory.read(address);
        if (debugger.isActive()) debugger.access(address, value, false);
        return value;
    }

    private void writeWord(int address, int value) {
        memory.write(address, value);
        if (debugger.isActive()) debugger.access(address, value, true);
    }

    @FunctionalInterface
    private interface Instruction {
        CPU.RunState execute() throws IOException;
    }
}
