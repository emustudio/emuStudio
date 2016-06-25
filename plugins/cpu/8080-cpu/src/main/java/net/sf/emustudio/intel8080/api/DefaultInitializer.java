package net.sf.emustudio.intel8080.api;

import emulib.emustudio.SettingsManager;
import emulib.plugins.Plugin;
import emulib.plugins.PluginInitializationException;
import emulib.plugins.cpu.Disassembler;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextNotFoundException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import net.sf.emustudio.intel8080.gui.DecoderImpl;
import net.sf.emustudio.intel8080.gui.DisassemblerImpl;

import java.util.Objects;

import static net.sf.emustudio.intel8080.impl.CpuImpl.PRINT_CODE;
import static net.sf.emustudio.intel8080.impl.CpuImpl.PRINT_CODE_USE_CACHE;

public abstract class DefaultInitializer<Engine extends CpuEngine> {
    private Disassembler disassembler;
    private Engine engine;
    private Plugin plugin;
    private long pluginId;
    private ContextPool contextPool;
    private SettingsManager settings;

    public DefaultInitializer(Plugin plugin, long pluginId, ContextPool contextPool, SettingsManager settings)
        throws PluginInitializationException {
        this.plugin = Objects.requireNonNull(plugin);
        this.pluginId = pluginId;
        this.contextPool = Objects.requireNonNull(contextPool);
        this.settings = Objects.requireNonNull(settings);
    }

    public final void initialize() throws PluginInitializationException {
        try {
            MemoryContext<Short> memory = contextPool.getMemoryContext(pluginId, MemoryContext.class);

            if (memory.getDataType() != Short.class) {
                throw new PluginInitializationException(
                    plugin,
                    "Operating memory type is not supported for this kind of CPU."
                );
            }

            // create disassembler and debug columns
            this.disassembler = new DisassemblerImpl(memory, new DecoderImpl(memory));
            this.engine = createEmulatorEngine(memory);

            String setting = settings.readSetting(pluginId, PRINT_CODE);
            String printCodeUseCache = settings.readSetting(pluginId, PRINT_CODE_USE_CACHE);
            if (setting != null && setting.toLowerCase().equals("true")) {
                if (printCodeUseCache == null || printCodeUseCache.toLowerCase().equals("true")) {
                    engine.setDispatchListener(createInstructionPrinter(disassembler, engine, true));
                } else {
                    engine.setDispatchListener(createInstructionPrinter(disassembler, engine, false));
                }
            }
        } catch (InvalidContextException | ContextNotFoundException e) {
            throw new PluginInitializationException(plugin, ": Could not get memory context", e);
        }
    }

    protected abstract Engine createEmulatorEngine(MemoryContext memory);

    protected abstract DispatchListener createInstructionPrinter(Disassembler disassembler, Engine engine, boolean useCache);

    public Disassembler getDisassembler() {
        return disassembler;
    }

    public Engine getEngine() {
        return engine;
    }
}
