/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.virtualcomputer;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.components.P;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.settings.PluginConfig;
import net.emustudio.application.settings.PluginConnection;
import net.emustudio.application.virtualcomputer.VirtualComputer.PluginMeta;
import net.emustudio.emulib.plugins.Plugin;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.FileExtension;
import net.emustudio.emulib.plugins.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import org.junit.Test;
import org.mockito.InOrder;

import javax.swing.*;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class VirtualComputerTest {

    @Test
    public void getCPUWorks() {
        CPU instance = mock(CPU.class);
        VirtualComputer vc = new VirtualComputer(mock(ComputerConfig.class), plugins(plugin(0L, pluginConfig("cpu", PLUGIN_TYPE.CPU), instance)));
        assertSame(instance, vc.getCPU().orElseThrow());
    }

    @Test
    public void getMemoryWorks() {
        Memory instance = mock(Memory.class);
        VirtualComputer vc = new VirtualComputer(mock(ComputerConfig.class), plugins(plugin(0L, pluginConfig("memory", PLUGIN_TYPE.MEMORY), instance)));
        assertSame(instance, vc.getMemory().orElseThrow());
    }

    @Test
    public void getCompilerWorks() {
        Compiler instance = mock(Compiler.class);
        VirtualComputer vc = new VirtualComputer(mock(ComputerConfig.class), plugins(plugin(0L, pluginConfig("compiler", PLUGIN_TYPE.COMPILER), instance)));
        assertSame(instance, vc.getCompiler().orElseThrow());
    }

    @Test
    public void getDevicesWorks() {
        Device d1 = mock(Device.class), d2 = mock(Device.class);
        VirtualComputer vc = new VirtualComputer(mock(ComputerConfig.class), plugins(
                plugin(0L, pluginConfig("device-1", PLUGIN_TYPE.DEVICE), d1),
                plugin(1L, pluginConfig("device-2", PLUGIN_TYPE.DEVICE), d2)
        ));
        assertEquals(List.of(d1, d2), vc.getDevices());
    }

    @Test
    public void gettersReturnEmptyWhenPluginTypeIsMissing() {
        VirtualComputer vc = new VirtualComputer(mock(ComputerConfig.class), new LinkedHashMap<>());
        assertTrue(vc.getCompiler().isEmpty());
        assertTrue(vc.getCPU().isEmpty());
        assertTrue(vc.getMemory().isEmpty());
        assertTrue(vc.getDevices().isEmpty());
    }

    @Test
    public void initializeSetsComputerAndInitializesPluginsInTypeOrder() throws Exception {
        Compiler compiler = mock(Compiler.class);
        Memory memory = mock(Memory.class);
        CPU cpu = mock(CPU.class);
        Device d1 = mock(Device.class), d2 = mock(Device.class);
        ContextPoolImpl contextPool = new ContextPoolImpl(0);

        Map<Long, PluginMeta> pluginMetas = new LinkedHashMap<>();
        pluginMetas.put(10L, pluginMeta(pluginConfig("device-1", PLUGIN_TYPE.DEVICE), d1));
        pluginMetas.put(20L, pluginMeta(pluginConfig("cpu", PLUGIN_TYPE.CPU), cpu));
        pluginMetas.put(30L, pluginMeta(pluginConfig("compiler", PLUGIN_TYPE.COMPILER), compiler));
        pluginMetas.put(40L, pluginMeta(pluginConfig("memory", PLUGIN_TYPE.MEMORY), memory));
        pluginMetas.put(50L, pluginMeta(pluginConfig("device-2", PLUGIN_TYPE.DEVICE), d2));

        VirtualComputer vc = new VirtualComputer(mock(ComputerConfig.class), pluginMetas);
        vc.initialize(contextPool);

        InOrder inOrder = inOrder(compiler, memory, cpu, d1, d2);
        inOrder.verify(compiler).initialize();
        inOrder.verify(memory).initialize();
        inOrder.verify(cpu).initialize();
        inOrder.verify(d1).initialize();
        inOrder.verify(d2).initialize();
    }

    @Test
    public void resetResetsAllPresentPlugins() {
        Compiler compiler = mock(Compiler.class);
        Memory memory = mock(Memory.class);
        CPU cpu = mock(CPU.class);
        Device device = mock(Device.class);

        VirtualComputer vc = new VirtualComputer(mock(ComputerConfig.class), plugins(
                plugin(0L, pluginConfig("compiler", PLUGIN_TYPE.COMPILER), compiler),
                plugin(1L, pluginConfig("memory", PLUGIN_TYPE.MEMORY), memory),
                plugin(2L, pluginConfig("cpu", PLUGIN_TYPE.CPU), cpu),
                plugin(3L, pluginConfig("device", PLUGIN_TYPE.DEVICE), device)
        ));
        vc.reset();

        verify(compiler).reset();
        verify(memory).reset();
        verify(cpu).reset();
        verify(device).reset();
    }

    @Test
    public void isConnectedHonorsDirectionality() {
        ComputerConfig computerConfig = mock(ComputerConfig.class);
        when(computerConfig.getConnections()).thenReturn(List.of(
                PluginConnection.create("compiler", "cpu", false, List.of()),
                PluginConnection.create("cpu", "device", true, List.of())
        ));

        VirtualComputer vc = new VirtualComputer(computerConfig, plugins(
                plugin(1L, pluginConfig("compiler", PLUGIN_TYPE.COMPILER), mock(Compiler.class)),
                plugin(2L, pluginConfig("cpu", PLUGIN_TYPE.CPU), mock(CPU.class)),
                plugin(3L, pluginConfig("device", PLUGIN_TYPE.DEVICE), mock(Device.class))
        ));

        assertTrue(vc.isConnected(1L, 2L));
        assertFalse(vc.isConnected(2L, 1L));
        assertTrue(vc.isConnected(2L, 3L));
        assertTrue(vc.isConnected(3L, 2L));
        assertFalse(vc.isConnected(1L, 3L));
    }

    @Test
    public void getComputerConfigAndCloseDelegateToUnderlyingConfig() {
        ComputerConfig computerConfig = mock(ComputerConfig.class);
        VirtualComputer vc = new VirtualComputer(computerConfig, new LinkedHashMap<>());

        assertSame(computerConfig, vc.getComputerConfig());
        vc.close();
        verify(computerConfig).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructPluginsCreatesTypedPluginInstancesWithPluginSettings() throws Exception {
        TestCompilerPlugin.lastPluginId = -1;
        TestCompilerPlugin.lastApplicationApi = null;
        TestCompilerPlugin.lastSettings = null;

        ApplicationApi applicationApi = mock(ApplicationApi.class);

        Map<Long, PluginMeta> result = VirtualComputer.constructPlugins(
                List.of((Class<Plugin>) (Class<?>) TestCompilerPlugin.class),
                List.of(pluginConfig("compiler", PLUGIN_TYPE.COMPILER)),
                applicationApi,
                new AppSettings(Config.inMemory(), true, false),
                () -> {}
        );

        assertEquals(List.of(1L), new ArrayList<>(result.keySet()));
        assertTrue(result.get(1L).pluginInstance instanceof TestCompilerPlugin);
        assertSame(applicationApi, TestCompilerPlugin.lastApplicationApi);
        assertEquals(1L, TestCompilerPlugin.lastPluginId);
        assertNotNull(TestCompilerPlugin.lastSettings);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructPluginsRejectsPluginThatDoesNotImplementExpectedInterface() {
        assertThrows(InvalidPluginException.class, () -> VirtualComputer.constructPlugins(
                List.of((Class<Plugin>) (Class<?>) NotACompilerPlugin.class),
                List.of(pluginConfig("compiler", PLUGIN_TYPE.COMPILER)),
                mock(ApplicationApi.class),
                new AppSettings(Config.inMemory(), true, false),
                () -> {}
        ));
    }

    @Test
    public void createPluginInstanceRejectsMissingProperConstructor() {
        assertThrows(InvalidPluginException.class, () ->
                VirtualComputer.createPluginInstance(1L, BrokenCompilerPlugin.class, mock(ApplicationApi.class), PluginSettings.UNAVAILABLE)
        );
    }

    @Test
    public void createPluginInstanceRejectsNullApplicationApi() {
        assertThrows(NullPointerException.class, () ->
                VirtualComputer.createPluginInstance(1L, TestCompilerPlugin.class, null, PluginSettings.UNAVAILABLE)
        );
    }

    // --- Helpers ---

    private static PluginConfig pluginConfig(String id, PLUGIN_TYPE pluginType) {
        return PluginConfig.create(id, pluginType, id, "/tmp/" + id + ".jar", P.of(0, 0), Config.inMemory());
    }

    private static Map<Long, PluginMeta> plugins(PluginEntry... entries) {
        Map<Long, PluginMeta> plugins = new LinkedHashMap<>();
        for (PluginEntry entry : entries) {
            plugins.put(entry.id, entry.meta);
        }
        return plugins;
    }

    private static PluginEntry plugin(long id, PluginConfig pluginConfig, Plugin instance) {
        return new PluginEntry(id, pluginMeta(pluginConfig, instance));
    }

    private static PluginMeta pluginMeta(PluginConfig pluginConfig, Plugin instance) {
        return new PluginMeta(PluginSettings.UNAVAILABLE, instance, pluginConfig);
    }

    private static final class PluginEntry {
        private final long id;
        private final PluginMeta meta;

        private PluginEntry(long id, PluginMeta meta) {
            this.id = id;
            this.meta = meta;
        }
    }

    // --- Minimal plugin stubs for reflection-based construction ---

    public static final class TestCompilerPlugin implements Compiler {
        static long lastPluginId = -1;
        static ApplicationApi lastApplicationApi;
        static PluginSettings lastSettings;

        public TestCompilerPlugin(long pluginId, ApplicationApi applicationApi, PluginSettings settings) {
            lastPluginId = pluginId;
            lastApplicationApi = applicationApi;
            lastSettings = settings;
        }

        @Override public void addCompilerListener(CompilerListener listener) {}
        @Override public void removeCompilerListener(CompilerListener listener) {}
        @Override public void compile(java.nio.file.Path inputPath, Optional<java.nio.file.Path> outputPath) {}
        @Override public LexicalAnalyzer createLexer() { return null; }
        @Override public List<FileExtension> getSourceFileExtensions() { return List.of(); }
        @Override public void reset() {}
        @Override public void initialize() {}
        @Override public void destroy() {}
        @Override public void showSettings(JFrame parent) {}
        @Override public boolean isShowSettingsSupported() { return false; }
        @Override public boolean isAutomationSupported() { return false; }
        @Override public String getTitle() { return "test"; }
        @Override public String getVersion() { return "test"; }
        @Override public String getCopyright() { return "test"; }
        @Override public String getDescription() { return "test"; }
    }

    public static final class NotACompilerPlugin implements Plugin {
        public NotACompilerPlugin(long pluginId, ApplicationApi applicationApi, PluginSettings settings) {}

        @Override public void reset() {}
        @Override public void initialize() {}
        @Override public void destroy() {}
        @Override public void showSettings(JFrame parent) {}
        @Override public boolean isShowSettingsSupported() { return false; }
        @Override public boolean isAutomationSupported() { return false; }
        @Override public String getTitle() { return "test"; }
        @Override public String getVersion() { return "test"; }
        @Override public String getCopyright() { return "test"; }
        @Override public String getDescription() { return "test"; }
    }

    public static final class BrokenCompilerPlugin implements Compiler {
        public BrokenCompilerPlugin() {} // wrong constructor

        @Override public void addCompilerListener(CompilerListener listener) {}
        @Override public void removeCompilerListener(CompilerListener listener) {}
        @Override public void compile(java.nio.file.Path inputPath, Optional<java.nio.file.Path> outputPath) {}
        @Override public LexicalAnalyzer createLexer() { return null; }
        @Override public List<FileExtension> getSourceFileExtensions() { return List.of(); }
        @Override public void reset() {}
        @Override public void initialize() {}
        @Override public void destroy() {}
        @Override public void showSettings(JFrame parent) {}
        @Override public boolean isShowSettingsSupported() { return false; }
        @Override public String getTitle() { return "broken"; }
        @Override public String getVersion() { return "broken"; }
        @Override public String getCopyright() { return "broken"; }
        @Override public String getDescription() { return "broken"; }
    }
}
