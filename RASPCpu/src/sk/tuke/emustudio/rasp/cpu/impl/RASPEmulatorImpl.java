/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.cpu.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.API;
import emulib.emustudio.SettingsManager;
import emulib.emustudio.debugtable.BreakpointColumn;
import emulib.emustudio.debugtable.DebugTable;
import emulib.emustudio.debugtable.MnemoColumn;
import emulib.plugins.PluginInitializationException;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.CPUContext;
import emulib.plugins.cpu.DebugColumn;
import emulib.plugins.cpu.Disassembler;
import emulib.runtime.AlreadyRegisteredException;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.StaticDialogs;
import java.util.ArrayList;
import java.util.Objects;
import javax.swing.JPanel;
import sk.tuke.emustudio.rasp.cpu.gui.LabelDebugColumn;
import sk.tuke.emustudio.rasp.cpu.gui.RASPCpuStatusPanel;
import sk.tuke.emustudio.rasp.cpu.gui.RASPDisassembler;
import sk.tuke.emustudio.rasp.memory.memoryitems.MemoryItem;
import sk.tuke.emustudio.rasp.memory.memoryitems.NumberMemoryItem;
import sk.tuke.emustudio.rasp.memory.memoryitems.RASPInstruction;
import sk.tuke.emustudio.rasp.memory.RASPMemoryContext;

@PluginType(
        type = PLUGIN_TYPE.CPU,
        title = "Random access stored program (RASP)",
        copyright = "",
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
        public RunState execute(NumberMemoryItem operand);
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

    private RunState read(NumberMemoryItem operand) {
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

    private RunState write_constant(NumberMemoryItem operand) {
        //just write the number, it is a constant
        context.getOutputTape().write(operand.toString());
        context.getOutputTape().moveRight();
        return RunState.STATE_STOPPED_BREAK;
    }

    private RunState write_register(NumberMemoryItem operand) {
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
    }

    @Override
    public void showSettings() {

    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    /**
     * Not needed method.
     *
     * @return empty string
     */
    @Override
    public String getVersion() {
        return "";
    }

    @Override
    public void reset(int addr) {
        IP = addr;
        super.reset(addr);
    }

}
