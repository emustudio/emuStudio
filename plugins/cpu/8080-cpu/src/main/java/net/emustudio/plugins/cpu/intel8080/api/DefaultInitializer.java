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

package net.emustudio.plugins.cpu.intel8080.api;

import net.emustudio.emulib.plugins.Plugin;
import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.PluginSettings;

import java.io.*;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public abstract class DefaultInitializer<Engine extends CpuEngine> {
    public static final String PRINT_CODE = "printCode";
    public static final String PRINT_CODE_USE_CACHE = "printCodeUseCache";
    public static final String PRINT_CODE_FILE_NAME = "printCodeFileName";

    private final Plugin plugin;
    private final long pluginId;
    private final ContextPool contextPool;
    private final PluginSettings settings;

    private Disassembler disassembler;
    private Engine engine;
    private boolean dumpInstructions;
    private PrintStream writer;

    public DefaultInitializer(Plugin plugin, long pluginId, ContextPool contextPool, PluginSettings settings) {
        this.plugin = Objects.requireNonNull(plugin);
        this.pluginId = pluginId;
        this.contextPool = Objects.requireNonNull(contextPool);
        this.settings = Objects.requireNonNull(settings);
    }

    @SuppressWarnings("unchecked")
    public final void initialize() throws PluginInitializationException {
        try {
            MemoryContext<Short> memory = contextPool.getMemoryContext(pluginId, MemoryContext.class);
            if (memory.getDataType() != Short.class) {
                throw new InvalidContextException(
                    "Unexpected memory cell type. Expected Short but was: " + memory.getDataType()
                );
            }

            // create disassembler and debug columns
            this.disassembler = createDisassembler(memory);
            this.engine = createEmulatorEngine(memory);

            boolean settingPrintCode = settings.getBoolean(PRINT_CODE, false);
            boolean printCodeUseCache = settings.getBoolean(PRINT_CODE_USE_CACHE, false);

            this.dumpInstructions = settingPrintCode;
            if (settingPrintCode) {
                String dumpFile = settings.getString(PRINT_CODE_FILE_NAME, "syserr");
                writer = (dumpFile.equals("syserr")) ? System.err : new PrintStream(new FileOutputStream(Path.of(dumpFile).toFile()), true);
                engine.setDispatchListener(createInstructionPrinter(disassembler, engine, printCodeUseCache, writer));
            }
        } catch (FileNotFoundException e) {
            throw new PluginInitializationException(plugin, "Could not find file?", e);
        }
    }

    public void destroy() {
        Optional.ofNullable(writer).ifPresent(PrintStream::close);
    }

    public Disassembler getDisassembler() {
        return disassembler;
    }

    public Engine getEngine() {
        return engine;
    }

    public boolean shouldDumpInstructions() {
        return dumpInstructions;
    }

    protected abstract Engine createEmulatorEngine(MemoryContext<Short> memory);

    protected abstract DispatchListener createInstructionPrinter(Disassembler disassembler, Engine engine,
                                                                 boolean useCache, PrintStream writer);

    protected abstract Disassembler createDisassembler(MemoryContext<Short> memory);
}
