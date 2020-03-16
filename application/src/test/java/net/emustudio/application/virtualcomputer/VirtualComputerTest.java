package net.emustudio.application.virtualcomputer;

import net.emustudio.application.configuration.ComputerConfig;
import net.emustudio.application.configuration.PluginConfig;
import net.emustudio.application.virtualcomputer.VirtualComputer.PluginMeta;
import net.emustudio.emulib.plugins.Plugin;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.PluginSettings;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VirtualComputerTest {

    @Test
    public void testGetCPUworks() {
        CPU instance = createNiceMock(CPU.class);
        replay(instance);
        PluginMeta meta = mockPluginMeta(PLUGIN_TYPE.CPU, instance);

        VirtualComputer virtualComputer = new VirtualComputer(mockComputerConfig(), mockPlugins(meta));
        Optional<CPU> cpu = virtualComputer.getCPU();

        assertTrue(cpu.isPresent());
        assertEquals(cpu.get(), instance);
    }

    @Test
    public void testGetMemoryWorks() {
        Memory instance = createNiceMock(Memory.class);
        replay(instance);
        PluginMeta meta = mockPluginMeta(PLUGIN_TYPE.MEMORY, instance);

        VirtualComputer virtualComputer = new VirtualComputer(mockComputerConfig(), mockPlugins(meta));
        Optional<Memory> memory = virtualComputer.getMemory();

        assertTrue(memory.isPresent());
        assertEquals(memory.get(), instance);
    }

    @Test
    public void testGetCompilerWorks() {
        Compiler instance = createNiceMock(Compiler.class);
        replay(instance);
        PluginMeta meta = mockPluginMeta(PLUGIN_TYPE.COMPILER, instance);

        VirtualComputer virtualComputer = new VirtualComputer(mockComputerConfig(), mockPlugins(meta));
        Optional<Compiler> compiler = virtualComputer.getCompiler();

        assertTrue(compiler.isPresent());
        assertEquals(compiler.get(), instance);
    }

    @Test
    public void testGetDevicesWorks() {
        Device instance1 = createNiceMock(Device.class);
        Device instance2 = createNiceMock(Device.class);
        replay(instance1, instance2);
        PluginMeta meta1 = mockPluginMeta(PLUGIN_TYPE.DEVICE, instance1);
        PluginMeta meta2 = mockPluginMeta(PLUGIN_TYPE.DEVICE, instance2);

        VirtualComputer virtualComputer = new VirtualComputer(mockComputerConfig(), mockPlugins(meta1, meta2));
        List<Device> devices = virtualComputer.getDevices();

        assertEquals(2, devices.size());
        assertEquals(instance1, devices.get(0));
        assertEquals(instance2, devices.get(1));
    }

    private ComputerConfig mockComputerConfig() {
        ComputerConfig computerConfig = createNiceMock(ComputerConfig.class);
        replay(computerConfig);
        return computerConfig;
    }

    private Map<Long, PluginMeta> mockPlugins(PluginMeta... meta) {
        Map<Long, PluginMeta> plugins = new HashMap<>();
        long i = 0;
        for (PluginMeta m : meta) {
            plugins.put(i++, m);
        }
        return plugins;
    }

    private PluginMeta mockPluginMeta(PLUGIN_TYPE pluginType, Plugin instance) {
        PluginConfig pluginConfig = createNiceMock(PluginConfig.class);
        expect(pluginConfig.getPluginType()).andReturn(pluginType).once();
        replay(pluginConfig);
        return new PluginMeta(PluginSettings.UNAVAILABLE, instance, pluginConfig);
    }
}
