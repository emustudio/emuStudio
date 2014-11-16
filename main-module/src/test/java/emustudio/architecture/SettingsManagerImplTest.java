/*
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2012-2014, Peter Jakubčo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package emustudio.architecture;

import emulib.plugins.PluginInitializationException;
import emulib.plugins.compiler.Compiler;
import emulib.plugins.cpu.CPU;
import emulib.plugins.device.Device;
import emulib.plugins.memory.Memory;
import emustudio.architecture.ComputerFactory.PluginInfo;
import emustudio.drawing.Schema;
import emustudio.main.CommandLineFactory;
import emustudio.main.Main;
import org.easymock.EasyMock;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SettingsManagerImplTest {

    private static CPU cpuMock;
    private static Memory memoryMock;
    private static Compiler compilerMock;
    private static Device deviceMock;

    @BeforeClass
    public static void setUpClass() {
        cpuMock = createNiceMock(CPU.class);
        memoryMock = createNiceMock(Memory.class);
        compilerMock = createNiceMock(Compiler.class);
        deviceMock = createNiceMock(Device.class);

        Main.commandLine = createNiceMock(CommandLineFactory.CommandLine.class);
        EasyMock.expect(Main.commandLine.autoWanted()).andReturn(Boolean.FALSE).anyTimes();
        EasyMock.expect(Main.commandLine.noGUIWanted()).andReturn(Boolean.FALSE).anyTimes();

        EasyMock.replay(cpuMock, memoryMock, compilerMock, deviceMock, Main.commandLine);
    }

    @AfterClass
    public static void tearDownClass() {
        Main.commandLine = null;
    }

    @Test
    public void testReadSetting() throws PluginInitializationException {
        Computer computer = createNiceMock(Computer.class);

        List<PluginInfo> pluginNames = new ArrayList<>();
        PluginInfo cpuInfo = new PluginInfo("cpu", "", "", Object.class, 0);
        pluginNames.add(cpuInfo);

        EasyMock.expect(computer.getPluginsInfo()).andReturn(pluginNames).anyTimes();
        EasyMock.replay(computer);

        Configuration configuration = createNiceMock(Configuration.class);
        expect(configuration.get("cpu.test")).andReturn("true").once();
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(computer, configuration);

        assertEquals("true", instance.readSetting(0, "test"));
        verify(computer, configuration);
    }

    @Test
    public void testGetDeviceName() throws PluginInitializationException {
        Computer computer = createNiceMock(Computer.class);
        EasyMock.expect(computer.getPluginsInfo()).andReturn(new ArrayList<PluginInfo>());
        EasyMock.replay(computer);

        Configuration configuration = createNiceMock(Configuration.class);
        expect(configuration.get("device0")).andReturn("deviceName").once();
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(computer, configuration);

        assertEquals("deviceName", instance.getDeviceName(0));
        verify(computer, configuration);
    }

    @Test
    public void testGetCompilerName() throws PluginInitializationException {
        Computer computer = createNiceMock(Computer.class);
        EasyMock.expect(computer.getPluginsInfo()).andReturn(new ArrayList<PluginInfo>());
        EasyMock.replay(computer);

        Configuration configuration = createNiceMock(Configuration.class);
        expect(configuration.get("compiler")).andReturn("compilerName").once();
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(computer, configuration);

        assertEquals("compilerName", instance.getCompilerName());
        verify(computer, configuration);
    }

    @Test
    public void testGetCPUName() throws PluginInitializationException {
        Computer computer = createNiceMock(Computer.class);
        EasyMock.expect(computer.getPluginsInfo()).andReturn(new ArrayList<PluginInfo>());
        EasyMock.replay(computer);

        Configuration configuration = createNiceMock(Configuration.class);
        expect(configuration.get("cpu")).andReturn("cpuName").once();
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(computer, configuration);

        assertEquals("cpuName", instance.getCPUName());
        verify(computer, configuration);
    }

    @Test
    public void testGetMemoryName() throws PluginInitializationException {
        Computer computer = createNiceMock(Computer.class);
        EasyMock.expect(computer.getPluginsInfo()).andReturn(new ArrayList<PluginInfo>());
        EasyMock.replay(computer);

        Configuration configuration = createNiceMock(Configuration.class);
        expect(configuration.get("memory")).andReturn("memoryName").once();
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(computer, configuration);

        assertEquals("memoryName", instance.getMemoryName());
        verify(computer,configuration);
    }

    @Test
    public void testWriteSetting() throws PluginInitializationException, WriteConfigurationException {
        Computer computer = createNiceMock(Computer.class);

        List<PluginInfo> pluginNames = new ArrayList<>();
        PluginInfo cpuInfo = new PluginInfo("cpu", "", "", Object.class, 0);
        pluginNames.add(cpuInfo);

        EasyMock.expect(computer.getPluginsInfo()).andReturn(pluginNames).anyTimes();
        EasyMock.replay(computer);

        Configuration configuration = createNiceMock(Configuration.class);
        configuration.set("cpu.test", "true");
        expectLastCall().once();
        configuration.write();
        expectLastCall().once();
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(computer, configuration);
        assertTrue(instance.writeSetting(0, "test", "true"));

        verify(computer, configuration);
    }

    @Test
    public void testRemoveSetting() throws PluginInitializationException {
        Computer computer = createNiceMock(Computer.class);

        List<PluginInfo> pluginNames = new ArrayList<>();
        PluginInfo cpuInfo = new PluginInfo("cpu", "", "", Object.class, 0);
        pluginNames.add(cpuInfo);

        EasyMock.expect(computer.getPluginsInfo()).andReturn(pluginNames).anyTimes();
        EasyMock.replay(computer);

        Configuration configuration = createNiceMock(Configuration.class);
        configuration.remove("cpu.test");
        expectLastCall().once();
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(computer, configuration);
        assertTrue(instance.removeSetting(0, "test"));

        verify(computer, configuration);
    }

    @Test
    public void testWriteSettingToAll() throws PluginInitializationException, WriteConfigurationException {
        Schema schema = createNiceMock(Schema.class);
        Computer computer = createNiceMock(Computer.class);

        List<PluginInfo> pluginNames = new ArrayList<>();
        PluginInfo cpuInfo = new PluginInfo("cpu", "", "", Object.class, 0);
        PluginInfo compilerInfo = new PluginInfo("compiler", "", "", Object.class, 1);

        pluginNames.add(cpuInfo);
        pluginNames.add(compilerInfo);

        EasyMock.expect(computer.getPluginsInfo()).andReturn(pluginNames).anyTimes();
        EasyMock.replay(computer);

        Configuration configuration = createNiceMock(Configuration.class);
        configuration.set("cpu.test", "true");
        expectLastCall().once();
        configuration.set("compiler.test", "true");
        expectLastCall().once();
        configuration.write();
        expectLastCall().times(2);
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(computer, configuration);
        assertTrue(instance.writeSetting("test", "true"));

        verify(computer, configuration);
    }
}
