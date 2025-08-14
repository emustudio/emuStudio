/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.emulib.plugins.Plugin;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.DefaultInitializer;
import net.emustudio.plugins.cpu.intel8080.api.DispatchListener;
import net.emustudio.plugins.cpu.zilogZ80.gui.DecoderImpl;
import net.emustudio.plugins.cpu.zilogZ80.gui.DisassemblerImpl;

import java.io.PrintStream;
import java.util.Objects;

public class InitializerZ80 extends DefaultInitializer<EmulatorEngine> {
    private final ContextZ80Impl context;

    public InitializerZ80(Plugin plugin, long pluginId, ContextPool contextPool, PluginSettings settings,
                          ContextZ80Impl context) {
        super(plugin, pluginId, contextPool, settings);
        this.context = Objects.requireNonNull(context);
    }

    @Override
    protected EmulatorEngine createEmulatorEngine(MemoryContext<Byte> memory) {
        return new EmulatorEngine(memory, context);
    }

    @Override
    protected DispatchListener createInstructionPrinter(Disassembler disassembler, EmulatorEngine engine,
                                                        boolean useCache, PrintStream printStream) {
        return new InstructionPrinter(disassembler, engine, useCache, printStream);
    }

    @Override
    protected Disassembler createDisassembler(MemoryContext<Byte> memory) {
        return new DisassemblerImpl(memory, new DecoderImpl(memory));
    }
}
