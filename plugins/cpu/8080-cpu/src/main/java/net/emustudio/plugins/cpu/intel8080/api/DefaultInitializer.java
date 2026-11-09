/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080.api;

import net.emustudio.emulib.plugin.PluginInitializationException;
import net.emustudio.emulib.plugin.cpu.Disassembler;
import net.emustudio.plugins.cpu.intel8080.MachineDebugging;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.ContextPool;

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

    public final void initialize() throws PluginInitializationException {
        MemoryContext<Byte> memory = contextPool.getMemory(pluginId, Byte.class);
        disassembler = createDisassembler(MachineDebugging.inspectionMemory(memory));
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
