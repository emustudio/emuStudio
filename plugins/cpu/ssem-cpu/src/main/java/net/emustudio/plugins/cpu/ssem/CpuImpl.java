/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;


import net.emustudio.emulib.Numbers;
import net.emustudio.emulib.plugin.PluginInitializationException;
import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.annotations.PluginRoot;
import net.emustudio.emulib.plugin.cpu.AbstractCPU;
import net.emustudio.emulib.plugin.cpu.CPUContext;
import net.emustudio.emulib.plugin.cpu.Decoder;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.emulib.application.ui.debugger.DebuggerColumn;
import net.emustudio.plugins.cpu.ssem.gui.CpuPanel;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@PluginRoot(
        type = PLUGIN_TYPE.CPU,
        title = "SSEM CPU",
        description = "Emulator of SSEM machine"
)
@SuppressWarnings("unused")
public class CpuImpl extends AbstractCPU implements net.emustudio.emulib.plugin.debugger.Debugging {
    private final CpuContextImpl context = new CpuContextImpl();
    private MemoryContext<Byte> memory;
    private Decoder disassembler;
    private EmulatorEngine engine;
    private CPUContext.CPUListener automationListener;
    private boolean snapshotPending;

    public CpuImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);
        applicationApi.getContextPool().register(pluginID, context, CPUContext.class);
    }

    @Override
    public void initialize() throws PluginInitializationException {
        memory = applicationApi.getContextPool().getMemory(pluginID, Byte.class);
        disassembler = new DecoderImpl(memory);
        engine = new EmulatorEngine(memory);
        context.setEngine(engine);
    }

    @Override
    public CompletableFuture<Void> startAutomation() {
        automationListener = new CPUContext.CPUListener() {
            private boolean running;

            @Override
            public void runStateChanged(RunState state) {
                if (running && state != RunState.STATE_RUNNING) snapshotPending = true;
                running = state == RunState.STATE_RUNNING;
            }

            @Override
            public void internalStateChanged() {
            }
        };
        context.addCPUListener(automationListener);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected void destroyInternal() {
        if (automationListener != null) context.removeCPUListener(automationListener);
        if (snapshotPending) {
            // Machine teardown reaches the CPU after the devices stop and before memory is released.
            try {
                writeAutomationSnapshot(Path.of("ssem.out"));
            } catch (IOException failure) {
                throw new UncheckedIOException("Could not write SSEM snapshot", failure);
            }
        }
    }

    void writeAutomationSnapshot(Path output) throws IOException {
        MemoryContext<Byte> snapshotMemory = MachineDebugging.inspectionMemory(memory);
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            writer.write("ACC=0x" + Integer.toHexString(engine.Acc.get()));
            writer.newLine();
            writer.write("CI=0x" + Integer.toHexString(engine.CI.get()));
            writer.newLine();
            writer.newLine();
            writer.write("   L L L L L 5 6 7 8 9 0 1 2 I I I 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1");
            writer.newLine();
            int words = snapshotMemory.getSize() / 4;
            for (int i = 0; i < words; i++) {
                int number = Numbers.readInt(snapshotMemory.read(i * 4, 4), Numbers.Strategy.BIG_ENDIAN);
                String binary = Numbers.formatBinary(number, 32);
                writer.write(String.format("%02d %s", i, binary.replace("0", "  ").replace("1", "* ")));
                writer.newLine();
            }
        }
    }

    private RunState stepInternal() {
        RunState result = engine.step();
        if (result == RunState.STATE_RUNNING) {
            return RunState.STATE_STOPPED_BREAK;
        } else {
            return result;
        }
    }

    @Override
    public List<DebuggerColumn<?>> getDebuggerColumns(Decoder instructions) {
        return List.of(
                DebuggerColumn.text("line", location -> String.format("%04X", location / 4)),
                DebuggerColumn.mnemo(instructions),
                DebuggerColumn.opcode(instructions)
        );
    }

    @Override
    public JPanel getStatusPanel() {
        if (applicationApi.getGUI() == null) {
            return null;
        }
        return new CpuPanel(engine, memory, applicationApi.getGUI(), context);
    }

    @Override
    public int getAddressSpaceSize() {
        return memory.getSize();
    }

    private boolean setInstructionLocationInternal(int location) {
        int memSize = memory.getSize();
        if (location < 0 || location >= memSize) {
            throw new IllegalArgumentException("Instruction position can be in <0," + memSize / 4 + ">, but was: " + location);
        }
        engine.CI.set(Math.max(0, location * 4 - 4));
        return true;
    }

    @Override
    public Expression compile(String source) { return engine.debugger.compile(source); }

    @Override
    public AutoCloseable observe(Observer observer, Capture capture) { return engine.debugger.observe(observer, capture); }

    @Override
    public Decoder getDecoder() {
        return disassembler;
    }

    private RunState executeInternal() {
        return engine.run();
    }

    private void resetInternal(int startPos) {
        engine.reset(startPos);
    }

    public EmulatorEngine getEngine() {
        return engine;
    }

    private void publishState(RunState state) {
        context.publishState(state, context.getInstructionLocation(), context.getCpuFrequencyKhz());
    }

    private void internalStateChanged() {
        publishState(context.getRunState());
    }

    @Override
    public RunState execute() {
        RunState result = RunState.STATE_STOPPED_BAD_INSTR;
        publishState(RunState.STATE_RUNNING);
        try {
            result = executeInternal();
            return result;
        } finally {
            publishState(result == RunState.STATE_RUNNING ? RunState.STATE_STOPPED_BREAK : result);
        }
    }

    @Override
    public RunState step() {
        RunState result = RunState.STATE_STOPPED_BAD_INSTR;
        publishState(RunState.STATE_RUNNING);
        try {
            result = stepInternal();
            return result;
        } finally {
            publishState(result == RunState.STATE_RUNNING ? RunState.STATE_STOPPED_BREAK : result);
        }
    }

    @Override
    public void reset(int startPos) {
        resetInternal(startPos);
        publishState(RunState.STATE_STOPPED_BREAK);
    }

    @Override
    public boolean setInstructionLocation(int location) {
        boolean changed = setInstructionLocationInternal(location);
        if (changed) internalStateChanged();
        return changed;
    }
}
