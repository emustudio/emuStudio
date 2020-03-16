/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2016-2017  Michal Šipoš
 * Copyright (C) 2020  Peter Jakubčo
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

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.AbstractCPU;
import net.emustudio.emulib.plugins.cpu.CPUContext;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.runtime.*;
import net.emustudio.emulib.runtime.interaction.debugger.BreakpointColumn;
import net.emustudio.emulib.runtime.interaction.debugger.DebuggerColumn;
import net.emustudio.emulib.runtime.interaction.debugger.DebuggerTable;
import net.emustudio.emulib.runtime.interaction.debugger.MnemoColumn;
import net.emustudio.plugins.cpu.rasp.gui.LabelDebugColumn;
import net.emustudio.plugins.cpu.rasp.gui.RASPCpuStatusPanel;
import net.emustudio.plugins.cpu.rasp.gui.RASPDisassembler;
import net.emustudio.plugins.device.abstracttape.api.AbstractTapeContext;
import net.emustudio.plugins.memory.rasp.NumberMemoryItem;
import net.emustudio.plugins.memory.rasp.api.MemoryItem;
import net.emustudio.plugins.memory.rasp.api.RASPInstruction;
import net.emustudio.plugins.memory.rasp.api.RASPMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@PluginRoot(
    type = PLUGIN_TYPE.CPU,
    title = "Random Access Stored Program (RASP) machine"
)
public class RASPEmulatorImpl extends AbstractCPU {
    private final static Logger LOGGER = LoggerFactory.getLogger(RASPEmulatorImpl.class);

    private RASPCpuContext context;
    private RASPMemoryContext memory;
    private RASPDisassembler disassembler;
    private RASPCpuStatusPanel gui;

    private boolean debugTableInitialized = false;
    private int IP; //instruction pointer

    public RASPEmulatorImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        ContextPool contextPool = applicationApi.getContextPool();
        this.context = new RASPCpuContext(contextPool);
        try {
            contextPool.register(pluginID, context, CPUContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException ex) {
            LOGGER.error("Could not register RASP CPU context", ex);
            applicationApi.getDialogs().showError(
                "Could not register RASP CPU context. Please see log file for more details", getTitle()
            );
        }

    }

    @Override
    protected void resetInternal(int startPos) {
        IP = startPos;
        loadInputs();
        context.getOutputTape().clear();
    }

    @Override
    protected void destroyInternal() {
        context.destroy();
    }

    @Override
    public RunState call() throws Exception {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                //if breakpoint is set, throw it
                if (isBreakpointSet(IP)) {
                    throw new Breakpoint();
                }
                //execute one step (executes one instruction of the CPU)
                RunState tmpRunState = stepInternal();

                /*if RunState.STATE_STOPPED_BREAK is returned, it means that 
                 the instruction was successful so just go on executing next instruction. 
                 If HALT was executed or something else was returned, just return it*/
                if (tmpRunState != RunState.STATE_STOPPED_BREAK) {
                    return tmpRunState;
                }
            } catch (IndexOutOfBoundsException e) {
                //can happen if trying to get instruction from invalid address
                return RunState.STATE_STOPPED_ADDR_FALLOUT;
            } catch (Breakpoint breakpoint) {
                //if breakpoint was set at particular instruction
                return RunState.STATE_STOPPED_BREAK;
            }
        }
        return RunState.STATE_STOPPED_NORMAL;
    }

    @Override
    protected RunState stepInternal() throws Exception {
        //get instruction
        MemoryItem item = memory.read(IP++);
        if (!(item instanceof RASPInstruction)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        RASPInstruction instruction = (RASPInstruction) item;

        //get the operand of the instruction
        item = memory.read(IP++);
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem operand = (NumberMemoryItem) item;

        int instructionCode = instruction.getCode();

        //if instruction is valid, execute it
        if (instructionCode >= RASPInstruction.READ && instructionCode <= RASPInstruction.HALT) {
            return executableInstructions[instruction.getCode()].execute(operand);
        }

        //if invalid, return "bad instruction"
        return RunState.STATE_STOPPED_BAD_INSTR;
    }

    @Override
    public JPanel getStatusPanel() {
        //also initialize debug table
        if (!debugTableInitialized) {
            DebuggerTable debugTable = applicationApi.getDebuggerTable();
            if (debugTable != null) {
                ArrayList<DebuggerColumn<?>> debugColumns = new ArrayList<>();
                debugColumns.add(new BreakpointColumn(this));
                debugColumns.add(new LabelDebugColumn(memory));
                debugColumns.add(new MnemoColumn(disassembler));
                debugTable.setDebuggerColumns(debugColumns);
            }
            debugTableInitialized = true;
        }
        if (gui == null) {
            gui = new RASPCpuStatusPanel(this);
        }
        return gui;
    }

    @Override
    public int getInstructionLocation() {
        return IP;
    }

    @Override
    public boolean setInstructionLocation(int location) {
        //if specified position is negative, it is invalid address
        if (location < 0) {
            return false;
        }
        IP = location;
        return true;
    }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }

    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().getMemoryContext(pluginID, RASPMemoryContext.class);
        disassembler = new RASPDisassembler(memory);
        context.init(pluginID);
        loadInputs();
    }

    @Override
    public void showSettings() {

    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public String getVersion() {
        return getResourceBundle().map(b -> b.getString("version")).orElse("(unknown)");
    }

    @Override
    public String getCopyright() {
        return getResourceBundle().map(b -> b.getString("copyright")).orElse("(unknown)");
    }

    @Override
    public String getDescription() {
        return "RASP machine emulator";
    }

    /**
     * Get current value of the accumulator (memory cell at address [0]).
     *
     * @return current value of the accumulator (memory cell at address [0])
     */
    public int getACC() {
        MemoryItem memoryItem = memory.read(0);
        if (memoryItem instanceof NumberMemoryItem) {
            return ((NumberMemoryItem) memoryItem).getValue();
        } else if (memoryItem instanceof RASPInstruction) {
            return ((RASPInstruction) memoryItem).getCode();
        } else {
            return 0;
        }
    }

    private void loadInputs() {
        AbstractTapeContext inputTape = context.getInputTape();
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
        RunState execute(NumberMemoryItem operand) throws IOException;
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

    private RunState read(NumberMemoryItem operand) throws IOException {
        //read from input tape
        String input = context.getInputTape().readData();
        //move the reading head forward
        context.getInputTape().moveRight();

        //try to get input, if invalid, show error and return "bad instruction"
        int inputInt;
        try {
            inputInt = Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            applicationApi.getDialogs().showError("Input tape only supports valid integer values.", getTitle());
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        //write input value to specified register
        memory.write(operand.getValue(), new NumberMemoryItem(inputInt));
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState write_constant(NumberMemoryItem operand) throws IOException {
        //just write the number, it is a constant
        context.getOutputTape().writeData(operand.toString());
        context.getOutputTape().moveRight();
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState write_register(NumberMemoryItem operand) throws IOException {
        /*get string representation of the item at given address, no matter
         if it is instruction or NumberMemoryItem
         */
        String memoryItemString = memory.read(operand.getValue()).toString();
        //write to output
        context.getOutputTape().writeData(memoryItemString);
        context.getOutputTape().moveRight();
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState load_constant(NumberMemoryItem operand) {
        memory.write(0, operand);
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState load_register(NumberMemoryItem operand) {
        memory.write(0, memory.read(operand.getValue()));
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState store(NumberMemoryItem operand) {
        memory.write(operand.getValue(), memory.read(0));
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState add_constant(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //write the sum to accumulator
        memory.write(0, new NumberMemoryItem(r0.getValue() + operand.getValue()));

        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState add_register(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //get i-th register
        item = memory.read(operand.getValue());
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem ri = (NumberMemoryItem) item;

        //write the sum to accumulator
        memory.write(0, new NumberMemoryItem(r0.getValue() + ri.getValue()));
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState sub_constant(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //write result to acc
        memory.write(0, new NumberMemoryItem(r0.getValue() - operand.getValue()));
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState sub_register(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //get i-th register
        item = memory.read(operand.getValue());
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem ri = (NumberMemoryItem) item;

        //write result to acc
        memory.write(0, new NumberMemoryItem(r0.getValue() - ri.getValue()));
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState mul_constant(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        memory.write(0, new NumberMemoryItem(r0.getValue() * operand.getValue()));

        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState mul_register(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //get i-th register
        item = memory.read(operand.getValue());
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem ri = (NumberMemoryItem) item;

        memory.write(0, new NumberMemoryItem(r0.getValue() * ri.getValue()));

        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState div_constant(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //prevent zero divison
        if (operand.getValue() == 0) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }

        memory.write(0, new NumberMemoryItem(r0.getValue() / operand.getValue()));
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState div_register(NumberMemoryItem operand) {
        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //get i-th register
        item = memory.read(operand.getValue());
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem ri = (NumberMemoryItem) item;

        //prevent zero division
        if (ri.getValue() == 0) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }

        memory.write(0, new NumberMemoryItem(r0.getValue() / ri.getValue()));
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState jmp(NumberMemoryItem operand) {
        //assign new value to IP (jump to address)
        IP = operand.getValue();
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState jz(NumberMemoryItem operand) {

        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //if accumulator is 0, jump to address
        if (r0.getValue() == 0) {
            IP = operand.getValue();
        }

        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState jgtz(NumberMemoryItem operand) {

        //get accumulator
        MemoryItem item = memory.read(0);
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        NumberMemoryItem r0 = (NumberMemoryItem) item;

        //if accumulator is greater than 0, jump to address
        if (r0.getValue() > 0) {
            IP = operand.getValue();
        }

        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState halt(NumberMemoryItem operand) {
        return RunState.STATE_STOPPED_NORMAL;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.cpu.rasp.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
