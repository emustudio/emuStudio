package net.emustudio.plugins.cpu.rasp;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.memory.rasp.NumberMemoryItem;
import net.emustudio.plugins.memory.rasp.api.MemoryItem;
import net.emustudio.plugins.memory.rasp.api.RASPInstruction;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryContext;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EmulatorEngine {
    private final Dialogs dialogs;
    private final RASPCpuContext cpu;
    private final RASPMemoryContext memory;

    public int IP; //instruction pointer

    public EmulatorEngine(RASPCpuContext cpu, RASPMemoryContext memory, Dialogs dialogs) {
        this.cpu = Objects.requireNonNull(cpu);
        this.memory = Objects.requireNonNull(memory);
        this.dialogs = Objects.requireNonNull(dialogs);
    }

    protected void reset(int location) {
        IP = location;
        loadInputs();
        cpu.getOutputTape().clear();
    }

    public CPU.RunState step() throws IOException {
        //get instruction
        MemoryItem item = memory.read(IP++);
        if (!(item instanceof RASPInstruction)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        RASPInstruction instruction = (RASPInstruction) item;

        //get the operand of the instruction
        item = memory.read(IP++);
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem operand = (NumberMemoryItem) item;

        int instructionCode = instruction.getCode();

        //if instruction is valid, execute it
        if (instructionCode >= RASPInstruction.READ && instructionCode <= RASPInstruction.HALT) {
            return executableInstructions[instruction.getCode()].execute(operand);
        }

        //if invalid, return "bad instruction"
        return CPU.RunState.STATE_STOPPED_BAD_INSTR;
    }

    public boolean setInstructionLocation(int location) {
        if (location < 0) {
            return false;
        }
        IP = location;
        return true;
    }

    public void loadInputs() {
        AbstractTapeContext inputTape = cpu.getInputTape();
        inputTape.clear();

        List<Integer> inputs = memory.getInputs();

        List<String> inputsStrings = inputs.stream().map(String::valueOf).collect(Collectors.toList());
        int j = inputs.size();
        for (int i = 0; i < j; i++) {
            inputTape.setSymbolAt(i, inputsStrings.get(i));
        }
    }


    /**
     * Interface representing an executable instruction.
     */
    private interface ExecutableInstruction {

        /**
         * Executes the CPU instruction, and returns running state of processor.
         *
         * @param operand the operand of the instruction
         * @return run state of CPU after trying to execute this instruction
         */
        CPU.RunState execute(NumberMemoryItem operand) throws IOException;
    }

    /**
     * Array of implementations of "execute()" method.
     */
    private ExecutableInstruction[] executableInstructions = new ExecutableInstruction[]{
        null,
        this::read,
        this::write_constant,
        this::write_register,
        this::load_constant,
        this::load_register,
        this::store,
        this::add_constant,
        this::add_register,
        this::sub_constant,
        this::sub_register,
        this::mul_constant,
        this::mul_register,
        this::div_constant,
        this::div_register,
        this::jmp,
        this::jz,
        this::jgtz,
        this::halt
    };

    private CPU.RunState read(NumberMemoryItem operand) throws IOException {
        //read from input tape
        String input = cpu.getInputTape().readData();
        //move the reading head forward
        cpu.getInputTape().moveRight();

        //try to get input, if invalid, show error and return "bad instruction"
        int inputInt;
        try {
            inputInt = Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            dialogs.showError("Input tape only supports valid integer values.", "Read input tape");
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        //write input value to specified register
        memory.write(operand.getValue(), new NumberMemoryItem(inputInt));
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState write_constant(NumberMemoryItem operand) throws IOException {
        //just write the number, it is a constant
        cpu.getOutputTape().writeData(operand.toString());
        cpu.getOutputTape().moveRight();
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState write_register(NumberMemoryItem operand) throws IOException {
        /*get string representation of the item at given address, no matter
         if it is instruction or NumberMemoryItem
         */
        String memoryItemString = memory.read(operand.getValue()).toString();
        //write to output
        cpu.getOutputTape().writeData(memoryItemString);
        cpu.getOutputTape().moveRight();
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState load_constant(NumberMemoryItem operand) {
        memory.write(0, operand);
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState load_register(NumberMemoryItem operand) {
        memory.write(0, memory.read(operand.getValue()));
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState store(NumberMemoryItem operand) {
        memory.write(operand.getValue(), memory.read(0));
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState add_constant(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //write the sum to accumulator
        memory.write(0, new NumberMemoryItem(r0.getValue() + operand.getValue()));

        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState add_register(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //get i-th register
        item = memory.read(operand.getValue());
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem ri = (NumberMemoryItem) item;

        //write the sum to accumulator
        memory.write(0, new NumberMemoryItem(r0.getValue() + ri.getValue()));
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState sub_constant(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //write result to acc
        memory.write(0, new NumberMemoryItem(r0.getValue() - operand.getValue()));
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState sub_register(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //get i-th register
        item = memory.read(operand.getValue());
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem ri = (NumberMemoryItem) item;

        //write result to acc
        memory.write(0, new NumberMemoryItem(r0.getValue() - ri.getValue()));
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState mul_constant(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        memory.write(0, new NumberMemoryItem(r0.getValue() * operand.getValue()));

        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState mul_register(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //get i-th register
        item = memory.read(operand.getValue());
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem ri = (NumberMemoryItem) item;

        memory.write(0, new NumberMemoryItem(r0.getValue() * ri.getValue()));

        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState div_constant(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //prevent zero divison
        if (operand.getValue() == 0) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }

        memory.write(0, new NumberMemoryItem(r0.getValue() / operand.getValue()));
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState div_register(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //get i-th register
        item = memory.read(operand.getValue());
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem ri = (NumberMemoryItem) item;

        //prevent zero division
        if (ri.getValue() == 0) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }

        memory.write(0, new NumberMemoryItem(r0.getValue() / ri.getValue()));
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState jmp(NumberMemoryItem operand) {
        //assign new value to IP (jump to address)
        IP = operand.getValue();
        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState jz(NumberMemoryItem operand) {

        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //if accumulator is 0, jump to address
        if (r0.getValue() == 0) {
            IP = operand.getValue();
        }

        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState jgtz(NumberMemoryItem operand) {

        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return CPU.RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //if accumulator is greater than 0, jump to address
        if (r0.getValue() > 0) {
            IP = operand.getValue();
        }

        return CPU.RunState.STATE_STOPPED_BREAK;
    }

    private CPU.RunState halt(NumberMemoryItem operand) {
        return CPU.RunState.STATE_STOPPED_NORMAL;
    }
}
