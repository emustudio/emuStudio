/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.intel8080.api;

import net.emustudio.emulib.plugins.Plugin;
import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
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
            MemoryContext<Byte> memory = contextPool.getMemoryContext(pluginId, MemoryContext.class);
            Class<?> cellTypeClass = memory.getCellTypeClass();
            if (cellTypeClass != Byte.class) {
                throw new InvalidContextException(
                        "Unexpected memory cell type. Expected Byte but was: " + cellTypeClass
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

    protected abstract Engine createEmulatorEngine(MemoryContext<Byte> memory);

    protected abstract DispatchListener createInstructionPrinter(Disassembler disassembler, Engine engine,
                                                                 boolean useCache, PrintStream writer);

    protected abstract Disassembler createDisassembler(MemoryContext<Byte> memory);
}
