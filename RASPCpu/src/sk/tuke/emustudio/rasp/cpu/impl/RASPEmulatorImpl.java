/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.cpu.impl;

import com.oracle.jrockit.jfr.ContentType;
import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.API;
import emulib.emustudio.SettingsManager;
import emulib.emustudio.debugtable.BreakpointColumn;
import emulib.emustudio.debugtable.DebugTable;
import emulib.emustudio.debugtable.MnemoColumn;
import emulib.plugins.PluginInitializationException;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.DebugColumn;
import emulib.plugins.cpu.Disassembler;
import emulib.runtime.AlreadyRegisteredException;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.StaticDialogs;
import java.util.ArrayList;
import java.util.Objects;
import javafx.animation.Animation;
import javax.swing.JPanel;
import sk.tuke.emustudio.rasp.cpu.gui.LabelDebugColumn;
import sk.tuke.emustudio.rasp.cpu.gui.RASPCpuStatusPanel;
import sk.tuke.emustudio.rasp.cpu.gui.RASPDisassembler;
import sk.tuke.emustudio.rasp.memory.MemoryItem;
import sk.tuke.emustudio.rasp.memory.NumberMemoryItem;
import sk.tuke.emustudio.rasp.memory.IntegerMemoryItem;
import sk.tuke.emustudio.rasp.memory.OperandType;
import sk.tuke.emustudio.rasp.memory.RASPInstruction;
import sk.tuke.emustudio.rasp.memory.impl.RASPMemoryContextImpl;

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
    private RASPMemoryContextImpl memory;
    private RASPDisassembler disassembler;

    private boolean debugTableInitialized = false;
    private int IP; //instruction pointer

    public RASPEmulatorImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
        context = new RASPCpuContext(this, contextPool);
        try {
            contextPool.register(pluginID, context, RASPCpuContext.class);
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
            memory.write(operand.getValue(), memory.read(getACC()));
            return RunState.STATE_STOPPED_BREAK;
        }
        return RunState.STATE_STOPPED_BAD_INSTR;
    }

    /**
     * Executes ADD instruction.
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
                if (isBreakpointSet(IP)) {
                    throw new Breakpoint();
                }
                RunState tmpRunState = stepInternal();
                if (tmpRunState != RunState.STATE_STOPPED_BREAK) {
                    return tmpRunState;
                }
            } catch (IndexOutOfBoundsException e) {
                return RunState.STATE_STOPPED_ADDR_FALLOUT;
            } catch (Breakpoint breakpoint) {
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

        MemoryItem item = memory.read(IP++);
        if (!(item instanceof RASPInstruction)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }

        RASPInstruction instruction = (RASPInstruction) item;

        item = memory.read(IP++);
        if (!(item instanceof NumberMemoryItem)) {
            return RunState.STATE_STOPPED_BAD_INSTR;
        }

        NumberMemoryItem operand = (NumberMemoryItem) item;

        int instructionCode = instruction.getCode();

        
    }

    @Override
    public JPanel getStatusPanel() {
        if (!debugTableInitialized) {
            DebugTable debugTable = API.getInstance().getDebugTable();
            if (debugTable != null) {
                ArrayList<DebugColumn> debugColumns = new ArrayList<>();
                debugColumns.add(new LabelDebugColumn(memory));
                debugColumns.add(new BreakpointColumn(this));
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
     * item is not a NumberMemoryItem, i.e. it is an instruction, -1 is
     * returned.
     *
     * @return current value of the accumulator (memory cell at address [0]), or
     * -1 if the item is not a NumberMemoryItem
     */
    public int getACC() {
        MemoryItem memoryItem = memory.read(0);
        if (memoryItem instanceof NumberMemoryItem) {
            return ((NumberMemoryItem) memoryItem).getValue();
        }
        return -1;
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
            memory = (RASPMemoryContextImpl) contextPool.getMemoryContext(getPluginID(), RASPMemoryContextImpl.class);
        } catch (InvalidContextException | ContextNotFoundException ex) {
            throw new PluginInitializationException(this, "Could not get memory context.", ex);
        }

        //check if memory is compatible with this CPU emulator
        if (memory.getDataType() != MemoryItem.class) {
            throw new PluginInitializationException(this, "Specified memory uses "
                    + "incompatible data type, this CPU emulator does not support such kind of memory.");
        }

        disassembler = new RASPDisassembler();
        context.init(getPluginID());

    }

    @Override
    public void showSettings() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        super.reset(addr);
        IP = addr;
    }

}
