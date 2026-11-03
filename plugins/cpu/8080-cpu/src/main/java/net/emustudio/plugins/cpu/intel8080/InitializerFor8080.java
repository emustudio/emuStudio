/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080;

import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.cpu.BankedDisassembler;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.plugins.cpu.intel8080.api.DefaultInitializer;
import net.emustudio.plugins.cpu.intel8080.api.FlowDisassembler;
import net.emustudio.plugins.cpu.intel8080.gui.DecoderImpl;
import net.emustudio.plugins.cpu.intel8080.gui.DisassemblerImpl;

import java.util.Objects;

public class InitializerFor8080 extends DefaultInitializer<EmulatorEngine> {
    private final Context8080Impl context;


    public InitializerFor8080(long pluginId, ContextPool contextPool,
                              Context8080Impl context) {
        super(pluginId, contextPool);
        this.context = Objects.requireNonNull(context);
    }

    @Override
    protected EmulatorEngine createEmulatorEngine(MemoryContext<Byte> memory) {
        return new EmulatorEngine(memory, context);
    }

    @Override
    protected Disassembler createDisassembler(MemoryContext<Byte> memory) {
        return new BankedDisassembler<>(memory, cells -> new FlowDisassembler(
                new DisassemblerImpl(cells, new DecoderImpl(cells)), cells, false));
    }
}
