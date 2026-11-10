/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.virtualcomputer;

import static net.emustudio.application.TestCpu.mockCpu;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.settings.PluginConfig;
import net.emustudio.application.settings.SchemaPoint;
import net.emustudio.application.settings.PluginConnection;
import net.emustudio.application.ApplicationApiImpl;
import net.emustudio.application.debugger.AnnotationIndex;
import net.emustudio.application.debugger.DebuggerSession;
import net.emustudio.application.virtualcomputer.VirtualComputer.PluginMeta;
import net.emustudio.emulib.plugin.Plugin;
import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.compiler.Compiler;
import net.emustudio.emulib.plugin.compiler.CompilerListener;
import net.emustudio.emulib.io.FileType;
import net.emustudio.emulib.plugin.compiler.LexicalAnalyzer;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.device.Device;
import net.emustudio.emulib.plugin.memory.Memory;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.emulib.application.ui.Dialogs;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;

import javax.swing.*;
import java.io.File;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class VirtualComputerTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    @SuppressWarnings("unchecked")
    public void initializationLoadsUserAnnotationsIntoSharedEngineWithoutOpeningDebuggerOrReadingMemory() throws Exception {
        java.nio.file.Path computerPath = temporary.getRoot().toPath().resolve("computer.toml");
        java.nio.file.Path debuggerPath = computerPath.resolveSibling(computerPath.getFileName() + ".debugger");
        CPU cpu = mockCpu();
        MemoryContext<Byte> memory = mock(MemoryContext.class);
        when(cpu.getTitle()).thenReturn("Test CPU");
        when(cpu.getAddressSpaceSize()).thenReturn(16);
        when(memory.getSize()).thenReturn(16);
        try (DebuggerSession seed = new DebuggerSession(cpu, net.emustudio.application.TestCpu.context(cpu), memory, debuggerPath)) {
            seed.annotations().add(new AnnotationIndex.Entry(3, "saved label", "CODE"));
            seed.save();
        }
        clearInvocations(memory);

        ContextPoolImpl contextPool = mock(ContextPoolImpl.class);
        doReturn(memory).when(contextPool).get(0, MemoryContext.class);
        doReturn(net.emustudio.application.TestCpu.context(cpu)).when(contextPool).get(0, net.emustudio.emulib.plugin.cpu.CPUContext.class);
        ApplicationApiImpl api = new ApplicationApiImpl(contextPool, mock(Dialogs.class), null);
        ComputerConfig config = mock(ComputerConfig.class);
        when(config.getPath()).thenReturn(computerPath);
        VirtualComputer computer = new VirtualComputer(config, plugins(plugin(1, pluginConfig("cpu", PLUGIN_TYPE.CPU), cpu)));
        java.lang.reflect.Field apiField = VirtualComputer.class.getDeclaredField("applicationApi");
        apiField.setAccessible(true);
        apiField.set(computer, api);

        try {
            computer.initialize(contextPool);

            assertTrue(api.getAnnotations().at(3).stream().anyMatch(entry -> entry.text.equals("saved label")));
            assertSame(api.getAnnotations(), computer.getDebugger().orElseThrow().annotationEngine);
            verify(memory, never()).read(anyInt());
            verify(memory, never()).peek(anyInt());
        } finally {
            computer.close();
        }
    }

    @Test
    public void getCPUWorks() {
        CPU instance = mockCpu();
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
        CPU cpu = mockCpu();
        Device d1 = mock(Device.class), d2 = mock(Device.class);
        ContextPoolImpl contextPool = new ContextPoolImpl(0);
        contextPool.register(20L, net.emustudio.application.TestCpu.context(cpu), net.emustudio.emulib.plugin.cpu.CPUContext.class);

        Map<Long, PluginMeta> pluginMetas = new LinkedHashMap<>();
        pluginMetas.put(10L, pluginMeta(pluginConfig("device-1", PLUGIN_TYPE.DEVICE), d1));
        pluginMetas.put(20L, pluginMeta(pluginConfig("cpu", PLUGIN_TYPE.CPU), cpu));
        pluginMetas.put(30L, pluginMeta(pluginConfig("compiler", PLUGIN_TYPE.COMPILER), compiler));
        pluginMetas.put(40L, pluginMeta(pluginConfig("memory", PLUGIN_TYPE.MEMORY), memory));
        pluginMetas.put(50L, pluginMeta(pluginConfig("device-2", PLUGIN_TYPE.DEVICE), d2));

        VirtualComputer vc = new VirtualComputer(mock(ComputerConfig.class), pluginMetas);
        vc.initialize(contextPool);
        assertSame(net.emustudio.application.TestCpu.context(cpu), vc.getExecution().orElseThrow().cpuContext());

        InOrder inOrder = inOrder(compiler, memory, cpu, d1, d2);
        inOrder.verify(compiler).initialize();
        inOrder.verify(memory).initialize();
        inOrder.verify(cpu).initialize();
        inOrder.verify(d1).initialize();
        inOrder.verify(d2).initialize();
    }

    @Test
    public void resetResetsAllPresentPlugins() throws Exception {
        Compiler compiler = mock(Compiler.class);
        Memory memory = mock(Memory.class);
        CPU cpu = mockCpu();
        Device device = mock(Device.class);

        VirtualComputer vc = new VirtualComputer(mock(ComputerConfig.class), plugins(
                plugin(0L, pluginConfig("compiler", PLUGIN_TYPE.COMPILER), compiler),
                plugin(1L, pluginConfig("memory", PLUGIN_TYPE.MEMORY), memory),
                plugin(2L, pluginConfig("cpu", PLUGIN_TYPE.CPU), cpu),
                plugin(3L, pluginConfig("device", PLUGIN_TYPE.DEVICE), device)
        ));
        ContextPoolImpl contextPool = new ContextPoolImpl(0);
        contextPool.register(2L, net.emustudio.application.TestCpu.context(cpu), net.emustudio.emulib.plugin.cpu.CPUContext.class);
        try {
            vc.initialize(contextPool);
            vc.reset();

            verify(compiler).reset();
            verify(memory).reset();
            verify(cpu).reset();
            verify(device).reset();
        } finally {
            vc.close();
        }
    }

    @Test
    public void memoryOnlyMachineProvidesHostOwnedEditsWithoutADebugger() throws Exception {
        Memory memory = mock(Memory.class);
        ContextPoolImpl contexts = new ContextPoolImpl(0);
        ApplicationApiImpl api = new ApplicationApiImpl(contexts, mock(Dialogs.class), null);
        VirtualComputer computer = new VirtualComputer(mock(ComputerConfig.class), plugins(
                plugin(1L, pluginConfig("memory", PLUGIN_TYPE.MEMORY), memory)));
        java.lang.reflect.Field apiField = VirtualComputer.class.getDeclaredField("applicationApi");
        apiField.setAccessible(true);
        apiField.set(computer, api);

        try {
            computer.initialize(contexts);
            assertTrue(computer.getCPU().isEmpty());
            assertTrue(computer.getDebugger().isEmpty());
            assertFalse(computer.getExecution().orElseThrow().isBusy());
            memory.reset();

            verify(memory).reset();
            assertEquals(-1, computer.getExecution().orElseThrow().getInstructionLocation());
        } finally {
            computer.close();
        }
        assertTrue(computer.getExecution().orElseThrow().isBusy());
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
                plugin(2L, pluginConfig("cpu", PLUGIN_TYPE.CPU), mockCpu()),
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
                pluginClassesByFile(List.of(pluginConfig("compiler", PLUGIN_TYPE.COMPILER)),
                        List.of((Class<Plugin>) (Class<?>) TestCompilerPlugin.class)),
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
                pluginClassesByFile(List.of(pluginConfig("compiler", PLUGIN_TYPE.COMPILER)),
                        List.of((Class<Plugin>) (Class<?>) NotACompilerPlugin.class)),
                List.of(pluginConfig("compiler", PLUGIN_TYPE.COMPILER)),
                mock(ApplicationApi.class),
                new AppSettings(Config.inMemory(), true, false),
                () -> {}
        ));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructPluginsPairsClassesByConfiguredPluginPath() throws Exception {
        PluginConfig compilerConfig = pluginConfig("compiler", PLUGIN_TYPE.COMPILER);
        PluginConfig memoryConfig = pluginConfig("memory", PLUGIN_TYPE.MEMORY);

        Map<File, Class<Plugin>> pluginClasses = new LinkedHashMap<>();
        pluginClasses.put(memoryConfig.getPluginPath().toFile(), (Class<Plugin>) (Class<?>) TestMemoryPlugin.class);
        pluginClasses.put(compilerConfig.getPluginPath().toFile(), (Class<Plugin>) (Class<?>) TestCompilerPlugin.class);

        Map<Long, PluginMeta> result = VirtualComputer.constructPlugins(
                pluginClasses,
                List.of(compilerConfig, memoryConfig),
                mock(ApplicationApi.class),
                new AppSettings(Config.inMemory(), true, false),
                () -> {}
        );

        assertTrue(result.get(1L).pluginInstance instanceof TestCompilerPlugin);
        assertTrue(result.get(2L).pluginInstance instanceof TestMemoryPlugin);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructPluginsCreatesInstancePerConfigurationOfSamePluginFile() throws Exception {
        PluginConfig first = pluginConfig("plugin", PLUGIN_TYPE.MEMORY);
        PluginConfig second = pluginConfig("plugin", PLUGIN_TYPE.MEMORY);
        Map<File, Class<Plugin>> pluginClasses = new LinkedHashMap<>();
        pluginClasses.put(first.getPluginPath().toFile(), (Class<Plugin>) (Class<?>) TestMemoryPlugin.class);

        Map<Long, PluginMeta> result = VirtualComputer.constructPlugins(
                pluginClasses,
                List.of(first, second),
                mock(ApplicationApi.class),
                new AppSettings(Config.inMemory(), true, false),
                () -> {}
        );

        assertEquals(2, result.size());
        assertNotSame(result.get(1L).pluginInstance, result.get(2L).pluginInstance);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructPluginsRejectsMismatchedPluginCounts() {
        PluginConfig compilerConfig = pluginConfig("compiler", PLUGIN_TYPE.COMPILER);
        Map<File, Class<Plugin>> pluginClasses = new LinkedHashMap<>();
        pluginClasses.put(compilerConfig.getPluginPath().toFile(), (Class<Plugin>) (Class<?>) TestCompilerPlugin.class);
        pluginClasses.put(new File("/tmp/extra.jar"), (Class<Plugin>) (Class<?>) TestCompilerPlugin.class);

        assertThrows(InvalidPluginException.class, () -> VirtualComputer.constructPlugins(
                pluginClasses,
                List.of(compilerConfig),
                mock(ApplicationApi.class),
                new AppSettings(Config.inMemory(), true, false),
                () -> {}
        ));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructionFailureDestroysAlreadyConstructedPluginsInReverseOrder() {
        TestCompilerPlugin.destroyedIds.clear();
        List<PluginConfig> configs = List.of(pluginConfig("first", PLUGIN_TYPE.COMPILER),
                pluginConfig("second", PLUGIN_TYPE.COMPILER), pluginConfig("broken", PLUGIN_TYPE.COMPILER));
        InvalidPluginException failure = assertThrows(InvalidPluginException.class,
                () -> VirtualComputer.constructPlugins(pluginClassesByFile(configs, List.of(
                        (Class<Plugin>) (Class<?>) TestCompilerPlugin.class,
                        (Class<Plugin>) (Class<?>) TestCompilerPlugin.class,
                        (Class<Plugin>) (Class<?>) BrokenCompilerPlugin.class)), configs,
                        mock(ApplicationApi.class), new AppSettings(Config.inMemory(), true, false), () -> {}));
        assertEquals("Plug-in main class does not have proper constructor", failure.getMessage());
        assertEquals(List.of(2L, 1L), TestCompilerPlugin.destroyedIds);
    }

    @Test
    public void initializationFailureClosesAllOwnedPluginsLoaderAndConfig() throws Exception {
        Compiler compiler = mock(Compiler.class);
        Memory memory = mock(Memory.class);
        Device device = mock(Device.class);
        AutoCloseable loader = mock(AutoCloseable.class);
        ComputerConfig config = mock(ComputerConfig.class);
        net.emustudio.emulib.plugin.PluginInitializationException failure =
                new net.emustudio.emulib.plugin.PluginInitializationException("bad memory");
        doThrow(failure).when(memory).initialize();
        // Even a broken cleanup must not hide the original initialization failure.
        doThrow(new AssertionError("destroy failed")).when(device).destroy();
        VirtualComputer computer = new VirtualComputer(config, plugins(
                plugin(1, pluginConfig("compiler", PLUGIN_TYPE.COMPILER), compiler),
                plugin(2, pluginConfig("memory", PLUGIN_TYPE.MEMORY), memory),
                plugin(3, pluginConfig("device", PLUGIN_TYPE.DEVICE), device)), loader);
        assertSame(failure, assertThrows(net.emustudio.emulib.plugin.PluginInitializationException.class,
                () -> computer.initialize(new ContextPoolImpl(0))));
        computer.close();
        verify(device).destroy();
        verify(memory).destroy();
        verify(compiler).destroy();
        verify(loader).close();
        verify(config).close();
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

    @Test
    public void closeDestroysPluginsInReverseOrderAndClosesClassLoader() throws Exception {
        Compiler compiler = mock(Compiler.class);
        Memory memory = mock(Memory.class);
        CPU cpu = mockCpu();
        Device d1 = mock(Device.class);
        Device d2 = mock(Device.class);
        AutoCloseable classLoader = mock(AutoCloseable.class);
        ComputerConfig computerConfig = mock(ComputerConfig.class);

        VirtualComputer vc = new VirtualComputer(computerConfig, plugins(
                plugin(0L, pluginConfig("compiler", PLUGIN_TYPE.COMPILER), compiler),
                plugin(1L, pluginConfig("memory", PLUGIN_TYPE.MEMORY), memory),
                plugin(2L, pluginConfig("cpu", PLUGIN_TYPE.CPU), cpu),
                plugin(3L, pluginConfig("device-1", PLUGIN_TYPE.DEVICE), d1),
                plugin(4L, pluginConfig("device-2", PLUGIN_TYPE.DEVICE), d2)
        ), classLoader);

        vc.close();

        InOrder inOrder = inOrder(d2, d1, cpu, memory, compiler, classLoader, computerConfig);
        inOrder.verify(d2).destroy();
        inOrder.verify(d1).destroy();
        inOrder.verify(cpu).destroy();
        inOrder.verify(memory).destroy();
        inOrder.verify(compiler).destroy();
        inOrder.verify(classLoader).close();
        inOrder.verify(computerConfig).close();
    }

    @Test
    public void closeContinuesAfterDestroyFailureAndIsIdempotent() throws Exception {
        Compiler compiler = mock(Compiler.class);
        Memory memory = mock(Memory.class);
        CPU cpu = mockCpu();
        Device badDevice = mock(Device.class);
        Device goodDevice = mock(Device.class);
        AutoCloseable classLoader = mock(AutoCloseable.class);
        ComputerConfig computerConfig = mock(ComputerConfig.class);
        doThrow(new RuntimeException("boom")).when(badDevice).destroy();

        VirtualComputer vc = new VirtualComputer(computerConfig, plugins(
                plugin(0L, pluginConfig("compiler", PLUGIN_TYPE.COMPILER), compiler),
                plugin(1L, pluginConfig("memory", PLUGIN_TYPE.MEMORY), memory),
                plugin(2L, pluginConfig("cpu", PLUGIN_TYPE.CPU), cpu),
                plugin(3L, pluginConfig("bad-device", PLUGIN_TYPE.DEVICE), badDevice),
                plugin(4L, pluginConfig("good-device", PLUGIN_TYPE.DEVICE), goodDevice)
        ), classLoader);

        vc.close();
        vc.close();

        verify(goodDevice).destroy();
        verify(badDevice).destroy();
        verify(cpu).destroy();
        verify(memory).destroy();
        verify(compiler).destroy();
        verify(classLoader).close();
        verify(computerConfig).close();
        verifyNoMoreInteractions(classLoader, computerConfig);
    }

    private static PluginConfig pluginConfig(String id, PLUGIN_TYPE pluginType) {
        return PluginConfig.create(id, pluginType, id, "/tmp/" + id + ".jar", SchemaPoint.of(0, 0), Config.inMemory());
    }

    private static Map<File, Class<Plugin>> pluginClassesByFile(List<PluginConfig> pluginConfigs, List<Class<Plugin>> pluginClasses) {
        Map<File, Class<Plugin>> result = new LinkedHashMap<>();
        for (int i = 0; i < pluginConfigs.size() && i < pluginClasses.size(); i++) {
            result.put(pluginConfigs.get(i).getPluginPath().toFile(), pluginClasses.get(i));
        }
        return result;
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

    public static final class TestCompilerPlugin implements Compiler {
        static long lastPluginId = -1;
        static ApplicationApi lastApplicationApi;
        static PluginSettings lastSettings;
        static final List<Long> destroyedIds = new ArrayList<>();
        private final long pluginId;

        public TestCompilerPlugin(long pluginId, ApplicationApi applicationApi, PluginSettings settings) {
            this.pluginId = pluginId;
            lastPluginId = pluginId;
            lastApplicationApi = applicationApi;
            lastSettings = settings;
        }

        @Override public void addCompilerListener(CompilerListener listener) {}
        @Override public void removeCompilerListener(CompilerListener listener) {}
        @Override public void compile(java.nio.file.Path inputPath, Optional<java.nio.file.Path> outputPath) {}
        @Override public LexicalAnalyzer createLexer() { return null; }
        @Override public List<FileType> getSourceFileTypes() { return List.of(); }
        @Override public void reset() {}
        @Override public void initialize() {}
        @Override public void destroy() { destroyedIds.add(pluginId); }
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

    public static final class TestMemoryPlugin implements Memory {
        public TestMemoryPlugin(long pluginId, ApplicationApi applicationApi, PluginSettings settings) {}

        @Override public void reset() {}
        @Override public void initialize() {}
        @Override public void destroy() {}
        @Override public void showSettings(JFrame parent) {}
        @Override public boolean isShowSettingsSupported() { return false; }
        @Override public String getTitle() { return "memory"; }
        @Override public String getVersion() { return "memory"; }
        @Override public String getCopyright() { return "memory"; }
        @Override public String getDescription() { return "memory"; }
    }

    public static final class BrokenCompilerPlugin implements Compiler {
        public BrokenCompilerPlugin() {} // wrong constructor

        @Override public void addCompilerListener(CompilerListener listener) {}
        @Override public void removeCompilerListener(CompilerListener listener) {}
        @Override public void compile(java.nio.file.Path inputPath, Optional<java.nio.file.Path> outputPath) {}
        @Override public LexicalAnalyzer createLexer() { return null; }
        @Override public List<FileType> getSourceFileTypes() { return List.of(); }
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
