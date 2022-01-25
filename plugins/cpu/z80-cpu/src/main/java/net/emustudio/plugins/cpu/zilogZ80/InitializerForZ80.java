/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.emustudio.plugins.cpu.zilogZ80;

import net.emustudio.emulib.plugins.Plugin;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.PluginSettings;
import net.emustudio.plugins.cpu.intel8080.api.DefaultInitializer;
import net.emustudio.plugins.cpu.intel8080.api.DispatchListener;
import net.emustudio.plugins.cpu.zilogZ80.gui.DecoderImpl;
import net.emustudio.plugins.cpu.zilogZ80.gui.DisassemblerImpl;

import java.io.PrintStream;
import java.util.Objects;

public class InitializerForZ80 extends DefaultInitializer<EmulatorEngine> {
    private final ContextImpl context;

    public InitializerForZ80(Plugin plugin, long pluginId, ContextPool contextPool, PluginSettings settings,
                             ContextImpl context) {
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
