/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2016, Michal Šipoš
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

package net.sf.emustudio.rasp.cpu.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.API;
import emulib.emustudio.SettingsManager;
import emulib.emustudio.debugtable.BreakpointColumn;
import emulib.emustudio.debugtable.DebugTable;
import emulib.emustudio.debugtable.MnemoColumn;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.CPUContext;
import emulib.plugins.cpu.DebugColumn;
import emulib.plugins.cpu.Disassembler;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import emulib.runtime.exceptions.AlreadyRegisteredException;
import emulib.runtime.exceptions.ContextNotFoundException;
import emulib.runtime.exceptions.InvalidContextException;
import emulib.runtime.exceptions.PluginInitializationException;
import net.sf.emustudio.devices.abstracttape.api.AbstractTapeContext;
import net.sf.emustudio.rasp.cpu.gui.LabelDebugColumn;
import net.sf.emustudio.rasp.cpu.gui.RASPCpuStatusPanel;
import net.sf.emustudio.rasp.cpu.gui.RASPDisassembler;
import net.sf.emustudio.rasp.memory.RASPMemoryContext;
import net.sf.emustudio.rasp.memory.memoryitems.MemoryItem;
import net.sf.emustudio.rasp.memory.memoryitems.NumberMemoryItem;
import net.sf.emustudio.rasp.memory.memoryitems.RASPInstruction;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@PluginType(
    type = PLUGIN_TYPE.CPU,
    title = "Random access stored program (RASP)",
    copyright = "\u00A9 Copyright 2016, Michal Šipoš",
    description = "CPU emulator for abstract RASP machine"
)
/**
 * CPU emulator implementation for abstract RASP machine.
 *
 * @author miso
 */
public class RASPEmulatorImpl extends AbstractCPU {

    private RASPCpuContext context;
    private ContextPool contextPool;
    private volatile SettingsManager settings;
    private RASPMemoryContext memory;
    private RASPDisassembler disassembler;
    private RASPCpuStatusPanel gui;

    private boolean debugTableInitialized = false;
    private int IP; //instruction pointer

    public RASPEmulatorImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
        context = new RASPCpuContext(this, contextPool);
        try {
            contextPool.register(pluginID, context, CPUContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException ex) {
            StaticDialogs.showErrorMessage("Could not register RASP CPU context", RASPEmulatorImpl.class.getAnnotation(PluginType.class).title());
        }

    }

    @Override
    protected void resetInternal(int startPos) {
        IP = startPos;
        loadInputs();
        context.getOutputTape().clear();
    }

    /**
     * Interface representing an executable instruction (strategy design
     * pattern).
     */
    private interface ExecutableInstruction {

        /**
         * Executes the CPU instruction, and returns running state of processor.
         *
         * @param operand the operand of the instruction
         * @return run state of CPU after trying to execute this instruction
         */
        public RunState execute(NumberMemoryItem operand) throws IOException;
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
        String input = context.getInputTape().read();
        //move the reading head forward
        context.getInputTape().moveRight();

        //try to get input, if invalid, show error and return "bad instruction"
        int inputInt;
        try {
            inputInt = Integer.valueOf(input);
        } catch (NumberFormatException exception) {
            StaticDialogs.showErrorMessage("The input tape only supports valid integer values.");
            return RunState.STATE_STOPPED_BAD_INSTR;
        }
        //write input value to specified register
        memory.write(operand.getValue(), new NumberMemoryItem(inputInt));
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState write_constant(NumberMemoryItem operand) throws IOException {
        //just write the number, it is a constant
        context.getOutputTape().write(operand.toString());
        context.getOutputTape().moveRight();
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState write_register(NumberMemoryItem operand) throws IOException {
        /*get string representation of the item at given address, no matter
         if it is instruction or NumberMemoryItem
         */
        String memoryItemString = memory.read(operand.getValue()).toString();
        //write to output
        context.getOutputTape().write(memoryItemString);
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

    /**
     * Destroy.
     */
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
        //if not both input and output tapes are attached, return address fallout
        if (!context.allTapesAreNonNull()) {
            return RunState.STATE_STOPPED_ADDR_FALLOUT;
        }

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
            DebugTable debugTable = API.getInstance().getDebugTable();
            if (debugTable != null) {
                ArrayList<DebugColumn> debugColumns = new ArrayList<>();
                debugColumns.add(new BreakpointColumn(this));
                debugColumns.add(new LabelDebugColumn(memory));
                debugColumns.add(new MnemoColumn(disassembler));
                debugTable.setCustomColumns(debugColumns);
            }
            debugTableInitialized = true;
        }
        gui = new RASPCpuStatusPanel(this, memory);
        return gui;
    }

    /**
     * Get current value of instruction pointer (IP).
     *
     * @return current value of instruction pointer (IP)
     */
    @Override
    public int getInstructionPosition() {
        return IP;
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

    /**
     * Set IP.
     *
     * @param position the position to set IP to
     * @return false if position is invalid
     */
    @Override
    public boolean setInstructionPosition(int position) {
        //if specified position is negative, it is invalid address
        if (position < 0) {
            return false;
        }
        IP = position;
        return true;
    }

    @Override
    public Disassembler getDisassembler() {
        return disassembler;
    }

    /**
     * Initialize the plugin.
     *
     * @param settings the settings to use
     * @throws PluginInitializationException
     */
    @Override
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        this.settings = settings;

        try {
            memory = (RASPMemoryContext) contextPool.getMemoryContext(getPluginID(), RASPMemoryContext.class);
        } catch (InvalidContextException | ContextNotFoundException ex) {
            throw new PluginInitializationException(this, "Could not get memory context.", ex);
        }

        //check if memory is compatible with this CPU emulator
        if (memory.getDataType() != MemoryItem.class) {
            throw new PluginInitializationException(this,
                "Specified memory uses "
                    + "incompatible data type, this CPU emulator does not support such kind of memory.");
        }

        disassembler = new RASPDisassembler(memory);
        context.init(getPluginID());
        loadInputs();
    }

    private void loadInputs() {
        AbstractTapeContext inputTape = context.getInputTape();
        inputTape.clear();

        List<Integer> inputs = memory.getInputs();

        List<String> inputsStrings = inputs.stream().map(i -> String.valueOf(i)).collect(Collectors.toList());
        int j = inputs.size();
        for (int i = 0; i < j; i++) {
            inputTape.setSymbolAt(i, inputsStrings.get(i));
        }
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
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.rasp.cpu.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

}
