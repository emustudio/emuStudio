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
package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.device.abstracttape.api.TapeSymbol;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static net.emustudio.plugins.memory.rasp.gui.Disassembler.HALT;

public class EmulatorEngine {
    public final AtomicInteger IP = new AtomicInteger();
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
        int opcode = memory.read(IP.getAndIncrement());
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
        int register = memory.read(IP.getAndIncrement());
        int input = inputTape.readData().number;

        inputTape.moveRight();
        memory.write(register, input);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState write_c() {
        int constant = memory.read(IP.getAndIncrement());
        outputTape.writeData(new TapeSymbol(constant));
        outputTape.moveRight();
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState write() {
        int register = memory.read(IP.getAndIncrement());
        int value = memory.read(register);
        outputTape.writeData(new TapeSymbol(value));
        outputTape.moveRight();
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState load_c() {
        int value = memory.read(IP.getAndIncrement());
        memory.write(0, value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState load() {
        int register = memory.read(IP.getAndIncrement());
        memory.write(0, memory.read(register));
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState store() {
        int register = memory.read(IP.getAndIncrement());
        memory.write(register, memory.read(0));
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState add_c() {
        int value = memory.read(IP.getAndIncrement());
        int r0 = memory.read(0);
        memory.write(0, r0 + value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState add() {
        int register = memory.read(IP.getAndIncrement());
        int value = memory.read(register);
        int r0 = memory.read(0);
        memory.write(0, r0 + value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState sub_c() {
        int value = memory.read(IP.getAndIncrement());
        int r0 = memory.read(0);
        memory.write(0, r0 - value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState sub() {
        int register = memory.read(IP.getAndIncrement());
        int value = memory.read(register);
        int r0 = memory.read(0);
        memory.write(0, r0 - value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState mul_c() {
        int value = memory.read(IP.getAndIncrement());
        int r0 = memory.read(0);
        memory.write(0, r0 * value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState mul() {
        int register = memory.read(IP.getAndIncrement());
        int value = memory.read(register);
        int r0 = memory.read(0);
        memory.write(0, r0 * value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState div_c() {
        int value = memory.read(IP.getAndIncrement());
        int r0 = memory.read(0);
        if (value == 0) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        memory.write(0, r0 / value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState div() {
        int register = memory.read(IP.getAndIncrement());
        int value = memory.read(register);
        int r0 = memory.read(0);
        if (value == 0) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        memory.write(0, r0 / value);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState jmp() {
        IP.set(memory.read(IP.get()));
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState jz() {
        int address = memory.read(IP.getAndIncrement());
        int r0 = memory.read(0);
        if (r0 == 0) {
            IP.set(address);
        }
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState jgtz() {
        int address = memory.read(IP.getAndIncrement());
        int r0 = memory.read(0);
        if (r0 > 0) {
            IP.set(address);
        }
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState halt() {
        return CPU.RunState.STATE_STOPPED_NORMAL;
    }

    @FunctionalInterface
    private interface Instruction {
        CPU.RunState execute() throws IOException;
    }
}
