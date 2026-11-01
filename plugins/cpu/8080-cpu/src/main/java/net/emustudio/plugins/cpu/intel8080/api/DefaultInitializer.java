/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080.api;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.InvalidContextException;

import java.util.Objects;

public abstract class DefaultInitializer<Engine> {
    private final long pluginId;
    private final ContextPool contextPool;
    private Disassembler disassembler;
    private Engine engine;

    public DefaultInitializer(long pluginId, ContextPool contextPool) {
        this.pluginId = pluginId;
        this.contextPool = Objects.requireNonNull(contextPool);
    }

    @SuppressWarnings("unchecked")
    public final void initialize() throws PluginInitializationException {
        MemoryContext<Byte> memory = contextPool.getMemoryContext(pluginId, MemoryContext.class);
        if (memory.getCellTypeClass() != Byte.class) {
            throw new InvalidContextException(
                    "Unexpected memory cell type. Expected Byte but was: " + memory.getCellTypeClass());
        }
        disassembler = createDisassembler(memory);
        engine = createEmulatorEngine(memory);
    }

    public Disassembler getDisassembler() {
        return disassembler;
    }

    public Engine getEngine() {
        return engine;
    }

    protected abstract Engine createEmulatorEngine(MemoryContext<Byte> memory);

    protected abstract Disassembler createDisassembler(MemoryContext<Byte> memory);
}
