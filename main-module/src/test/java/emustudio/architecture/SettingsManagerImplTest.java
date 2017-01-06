/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.architecture;

import emulib.plugins.Plugin;
import emulib.runtime.exceptions.PluginInitializationException;
import emustudio.architecture.ComputerFactory.PluginInfo;
import emustudio.main.CommandLine;
import emustudio.main.Main;
import org.easymock.EasyMock;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SettingsManagerImplTest {

    @BeforeClass
    public static void setUpClass() {
        Main.commandLine = createNiceMock(CommandLine.class);
        EasyMock.expect(Main.commandLine.isAuto()).andReturn(Boolean.FALSE).anyTimes();
        EasyMock.expect(Main.commandLine.isNoGUI()).andReturn(Boolean.FALSE).anyTimes();

        EasyMock.replay(Main.commandLine);
    }

    @AfterClass
    public static void tearDownClass() {
        Main.commandLine = null;
    }

    @Test
    public void testReadSetting() throws PluginInitializationException {
        List<PluginInfo> pluginNames = Collections.singletonList(stubPluginInfo("cpu", "8080-cpu", 0));

        Configuration configuration = createNiceMock(Configuration.class);
        expect(configuration.get("cpu.test")).andReturn("true").once();
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(pluginNames, configuration);

        assertEquals("true", instance.readSetting(0, "test"));
        verify(configuration);
    }

    @Test
    public void testGetDeviceName() throws PluginInitializationException {
        Configuration configuration = createNiceMock(Configuration.class);
        expect(configuration.get("device0")).andReturn("deviceName").once();
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(Collections.emptyList(), configuration);

        assertEquals("deviceName", instance.getDeviceName(0));
        verify(configuration);
    }

    @Test
    public void testGetCompilerName() throws PluginInitializationException {
        Configuration configuration = createNiceMock(Configuration.class);
        expect(configuration.get("compiler")).andReturn("compilerName").once();
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(Collections.emptyList(), configuration);

        assertEquals("compilerName", instance.getCompilerName());
        verify(configuration);
    }

    @Test
    public void testGetCPUName() throws PluginInitializationException {
        Configuration configuration = createNiceMock(Configuration.class);
        expect(configuration.get("cpu")).andReturn("cpuName").once();
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(Collections.emptyList(), configuration);

        assertEquals("cpuName", instance.getCPUName());
        verify(configuration);
    }

    @Test
    public void testGetMemoryName() throws PluginInitializationException {
        Configuration configuration = createNiceMock(Configuration.class);
        expect(configuration.get("memory")).andReturn("memoryName").once();
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(Collections.emptyList(), configuration);

        assertEquals("memoryName", instance.getMemoryName());
        verify(configuration);
    }

    @Test
    public void testWriteSetting() throws PluginInitializationException, WriteConfigurationException {
        List<PluginInfo> pluginNames = Collections.singletonList(stubPluginInfo("cpu", "8080-cpu", 0));

        Configuration configuration = createNiceMock(Configuration.class);
        configuration.set("cpu.test", "true");
        expectLastCall().once();
        configuration.write();
        expectLastCall().once();
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(pluginNames, configuration);
        assertTrue(instance.writeSetting(0, "test", "true"));

        verify(configuration);
    }

    @Test
    public void testRemoveSetting() throws PluginInitializationException {
        List<PluginInfo> pluginNames = Collections.singletonList(stubPluginInfo("cpu", "8080-cpu", 0));

        Configuration configuration = createNiceMock(Configuration.class);
        configuration.remove("cpu.test");
        expectLastCall().once();
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(pluginNames, configuration);
        assertTrue(instance.removeSetting(0, "test"));

        verify(configuration);
    }

    @Test
    public void testWriteSettingToAll() throws PluginInitializationException, WriteConfigurationException {
        List<PluginInfo> pluginNames = Arrays.asList(
            stubPluginInfo("cpu", "8080-cpu", 0), stubPluginInfo("compiler", "as-8080", 1)
        );

        Configuration configuration = createNiceMock(Configuration.class);
        configuration.set("cpu.test", "true");
        expectLastCall().once();
        configuration.set("compiler.test", "true");
        expectLastCall().once();
        configuration.write();
        expectLastCall().times(2);
        replay(configuration);

        SettingsManagerImpl instance = new SettingsManagerImpl(pluginNames, configuration);
        assertTrue(instance.writeSetting("test", "true"));

        verify(configuration);
    }

    private PluginInfo stubPluginInfo(String pluginConfigName, String pluginName, long pluginId) {
        return new PluginInfo<>(pluginConfigName, pluginName, pluginId, new File(""), Plugin.class);
    }
}
