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
     * Destroy.
     */
    @Override
    protected void destroyInternal() {
        context.destroy();
    }

    @Override
    protected RunState stepInternal() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    public RunState call() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void reset(int addr) {
        super.reset(addr);
        IP = addr;
    }

}
