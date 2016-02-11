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
import sk.tuke.emustudio.rasp.memory.MemoryItem;
import sk.tuke.emustudio.rasp.memory.NumberMemoryItem;
import sk.tuke.emustudio.rasp.memory.OperandType;
import sk.tuke.emustudio.rasp.memory.RASPInstruction;
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
         * @param operandType the type of the operand
         * @return run state of CPU after trying to execute this instruction
         */
        public RunState execute(NumberMemoryItem operand, OperandType operandType);
    }

    /**
     * Array of implementations of "execute()" method.
     */
    private ExecutableInstruction[] executableInstructions = new ExecutableInstruction[]{
        null,
        this::read,
        this::write,
        this::load,
        this::store,
        this::add,
        this::sub,
        this::mul,
        this::div,
        this::jmp,
        this::jz,
        this::jgtz,
        this::halt
    };

    /**
     * Executes READ instruction.
     *
     * @param operand the register to read to
     * @param operandType the type of the operand (should be register)
     * @return run state after executing the instruction
     */
    private RunState read(NumberMemoryItem operand, OperandType operandType) {
        if (operandType == OperandType.REGISTER) {
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
            //write to input value to specified register
            memory.write(operand.getValue(), new NumberMemoryItem(inputInt));
            return RunState.STATE_STOPPED_BREAK;
        }
        return RunState.STATE_STOPPED_BAD_INSTR;
    }

    /**
     * Executes WRITE instruction.
     *
     * @param operand the register to read value from, or constant; this value
     * will be written to output
     * @param operandType the type of the operand
     * @return run state after executing the instruction
     */
    private RunState write(NumberMemoryItem operand, OperandType operandType) {
        if (operandType == OperandType.REGISTER) {
            /*get string representation of the item at given address, no matter
             if it is instruction or NumberMemoryItem
             */
            String memoryItemString = memory.read(operand.getValue()).toString();
            //write to output
            context.getOutputTape().write(memoryItemString);
            context.getOutputTape().moveRight();
            return RunState.STATE_STOPPED_BREAK;
        } else if (operandType == OperandType.CONSTANT) {
            //just write the number, it is a constant
            context.getOutputTape().write(operand.toString());
            context.getOutputTape().moveRight();
            return RunState.STATE_STOPPED_BREAK;
        }
        return RunState.STATE_STOPPED_BAD_INSTR;
    }

    /**
     * Executes LOAD instruction.
     *
     * @param operand the register to read value from, or constatnt value; this
     * value will be written to accumulator
     * @param operandType the type of the operand
     * @return run state after executing the instruction
     */
    private RunState load(NumberMemoryItem operand, OperandType operandType) {
        if (operandType == OperandType.REGISTER) {
            memory.write(0, memory.read(operand.getValue()));
            return RunState.STATE_STOPPED_BREAK;
        } else if (operandType == OperandType.CONSTANT) {
            memory.write(0, operand);
            return RunState.STATE_STOPPED_BREAK;
        }
        return RunState.STATE_STOPPED_BAD_INSTR;
    }

    /**
     * Executes STORE instruction.
     *
     * @param operand the register to store accumulator to
     * @param operandType the type of the operand
     * @return run state after executing the instruction
     */
    private RunState store(NumberMemoryItem operand, OperandType operandType) {
        if (operandType == OperandType.REGISTER) {
            memory.write(operand.getValue(), memory.read(0));
            return RunState.STATE_STOPPED_BREAK;
        }
        return RunState.STATE_STOPPED_BAD_INSTR;
    }

    /**
     * Executes ADD instruction.
     *
     * @param operand register or constant
     * @param operandType the type of the operand
     * @return run state after executing the instruction
     */
    private RunState add(NumberMemoryItem operand, OperandType operandType) {
        if (operandType == OperandType.REGISTER) {

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
        } else if (operandType == OperandType.CONSTANT) {

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

        return RunState.STATE_STOPPED_BAD_INSTR;
    }

    /**
     * Executes SUB instruction.
     *
     * @param operand register or constant
     * @param operandType the type of the operand
     * @return run state after executing the instruction
     */
    private RunState sub(NumberMemoryItem operand, OperandType operandType) {
        if (operandType == OperandType.REGISTER) {
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
        } else if (operandType == OperandType.CONSTANT) {
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
        return RunState.STATE_STOPPED_BAD_INSTR;
    }

    /**
     * Executes MUL instruction.
     *
     * @param operand register or constant
     * @param operandType the type of the operand
     * @return run state after executing the instruction
     */
    private RunState mul(NumberMemoryItem operand, OperandType operandType) {
        if (operandType == OperandType.REGISTER) {

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
        } else if (operandType == OperandType.CONSTANT) {
            //get accumulator
            MemoryItem item = memory.read(0);
            if (!(item instanceof NumberMemoryItem)) {
                return RunState.STATE_STOPPED_BAD_INSTR;
            }
            NumberMemoryItem r0 = (NumberMemoryItem) item;

            memory.write(0, new NumberMemoryItem(r0.getValue() * operand.getValue()));

            return RunState.STATE_STOPPED_BREAK;
        }
        return RunState.STATE_STOPPED_BAD_INSTR;
    }

    /**
     * Executes DIV instruction.
     *
     * @param operand register or constant
     * @param operandType the type of the operand
     * @return run state after executing the instruction
     */
    private RunState div(NumberMemoryItem operand, OperandType operandType) {
        if (operandType == OperandType.REGISTER) {
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
        } else if (operandType == OperandType.CONSTANT) {
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
        return RunState.STATE_STOPPED_BAD_INSTR;
    }

    /**
     * Executes JMP instruction.
     *
     * @param operand register (labeled address in memory)
     * @param operandType the type of the operand, should be a register
     * @return run state after executing the instruction
     */
    private RunState jmp(NumberMemoryItem operand, OperandType operandType) {
        if (operandType == OperandType.REGISTER) {
            //assign new value to IP (jump to address)
            IP = operand.getValue();
            return RunState.STATE_STOPPED_BREAK;
        }
        return RunState.STATE_STOPPED_BAD_INSTR;
    }

    /**
     * Executes JZ instruction.
     *
     * @param operand register (labeled address in memory)
     * @param operandType the type of the operand, should be a register
     * @return run state after executing the instruction
     */
    private RunState jz(NumberMemoryItem operand, OperandType operandType) {
        if (operandType == OperandType.REGISTER) {

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
        return RunState.STATE_STOPPED_BAD_INSTR;
    }

    /**
     * Executes JGTZ instruction.
     *
     * @param operand register (labeled address in memory)
     * @param operandType the type of the operand, should be a register
     * @return run state after executing the instruction
     */
    private RunState jgtz(NumberMemoryItem operand, OperandType operandType) {
        if (operandType == OperandType.REGISTER) {

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
        return RunState.STATE_STOPPED_BAD_INSTR;
    }

    /**
     * Executes HALT instruction, no need for checking parameters, they are not
     * used.
     *
     * @return run state after executing the insstruction, i.e.
     * RunState.STATE_STOPPED_NORMAL
     */
    private RunState halt(NumberMemoryItem operand, OperandType operandType) {
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
            return executableInstructions[instruction.getCode()].execute(operand, instruction.getOperandType());
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
        return new RASPCpuStatusPanel(this, memory);
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
     * Get current value of the accumulator (memory cell at address [0]). If the
     * item is not a NumberMemoryItem, i.e. it is an instruction, its opcode is returned.
     *
     * @return current value of the accumulator (memory cell at address [0]), or
     * the opcode if the item is a RASPInstruction
     */
    public int getACC() {
        MemoryItem memoryItem = memory.read(0);
        if (memoryItem instanceof NumberMemoryItem) {
            return ((NumberMemoryItem) memoryItem).getValue();
        }
        //if RASPInstruction is located in the ACC, return its opcode
        return ((RASPInstruction) memoryItem).getCode();
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
