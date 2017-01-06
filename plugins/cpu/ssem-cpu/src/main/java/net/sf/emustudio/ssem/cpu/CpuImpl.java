/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.ssem.cpu;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.API;
import emulib.emustudio.SettingsManager;
import emulib.emustudio.debugtable.BreakpointColumn;
import emulib.emustudio.debugtable.DebugTable;
import emulib.emustudio.debugtable.MnemoColumn;
import emulib.emustudio.debugtable.OpcodeColumn;
import emulib.plugins.cpu.AbstractCPU;
import emulib.plugins.cpu.Decoder;
import emulib.plugins.cpu.Disassembler;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import emulib.runtime.exceptions.PluginInitializationException;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.swing.JPanel;
import net.sf.emustudio.ssem.DecoderImpl;
import net.sf.emustudio.ssem.DisassemblerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginType(
    type = PLUGIN_TYPE.CPU,
    title = "SSEM CPU",
    copyright = "\u00A9 Copyright 2006-2017, Peter Jakubčo",
    description = "Emulator of SSEM CPU"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU {
    private final static Logger LOGGER = LoggerFactory.getLogger(CpuImpl.class);

    private final ContextPool contextPool;
    private MemoryContext<Byte> memory;
    private Disassembler disasm;
    private EmulatorEngine engine;

    public CpuImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
    }

    @Override
    protected void destroyInternal() {

    }

    @Override
    protected RunState stepInternal() throws Exception {
        return engine.step();
    }

    @Override
    public JPanel getStatusPanel() {
        DebugTable debugTable = API.getInstance().getDebugTable();
        if (debugTable != null) {
            debugTable.setCustomColumns(Arrays.asList(
                    new BreakpointColumn(this), new LineColumn(), new MnemoColumn(disasm),
                    new OpcodeColumn(disasm)
            ));
        }
       
        return new CpuPanel(this, engine, memory);
    }

    @Override
    public int getInstructionPosition() {
        return engine.CI;
    }

    @Override
    public boolean setInstructionPosition(int i) {
        int memSize = memory.getSize();
        if (i < 0 || i >= memSize) {
            throw new IllegalArgumentException("Instruction position can be in <0," + memSize/4 +">, but was: " + i);
        }
        engine.CI = i;
        return true;
    }

    @Override
    public Disassembler getDisassembler() {
        return disasm;
    }

    @Override
    public void initialize(SettingsManager settingsManager) throws PluginInitializationException {
        memory = contextPool.getMemoryContext(getPluginID(), MemoryContext.class);
        Decoder decoder = new DecoderImpl(memory);
        disasm = new DisassemblerImpl(memory, decoder);
        engine = new EmulatorEngine(memory, this);
    }

    @Override
    public String getVersion() {
       try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.ssem.cpu.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            LOGGER.error("Could not load resource file", e);
            return "(unknown)";
        }
    }

    @Override
    public RunState call() throws Exception {
        return engine.run();
    }

    @Override
    protected void resetInternal(int startPos) {
        engine.reset(startPos);
    }

    public EmulatorEngine getEngine() {
        return engine;
    }
}
