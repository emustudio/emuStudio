package net.sf.emustudio.zilogZ80.impl;

import emulib.emustudio.SettingsManager;
import emulib.plugins.Plugin;
import emulib.plugins.PluginInitializationException;
import emulib.plugins.cpu.Disassembler;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import net.sf.emustudio.intel8080.api.DefaultInitializer;
import net.sf.emustudio.intel8080.api.DispatchListener;

import java.util.Objects;

public class InitializerForZ80 extends DefaultInitializer<EmulatorEngine> {
    private final ContextImpl context;

    public InitializerForZ80(Plugin plugin, long pluginId, ContextPool contextPool, SettingsManager settings,
                             ContextImpl context) throws PluginInitializationException {
        super(plugin, pluginId, contextPool, settings);
        this.context = Objects.requireNonNull(context);
    }

    @Override
    protected EmulatorEngine createEmulatorEngine(MemoryContext memory) {
        return new EmulatorEngine(memory, context);
    }

    @Override
    protected DispatchListener createInstructionPrinter(Disassembler disassembler, EmulatorEngine engine, boolean useCache) {
        return new InstructionPrinter(disassembler, engine, useCache);
    }
}
