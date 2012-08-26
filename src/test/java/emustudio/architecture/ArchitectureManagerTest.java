/*
 * ArchitectureManagerTest.java
 * 
 * KISS, YAGNI, DRY
 * 
 * Copyright (C) 2012, Peter Jakubčo
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

import emulib.plugins.compiler.Compiler;
import emulib.plugins.cpu.CPU;
import emulib.plugins.device.Device;
import emulib.plugins.memory.Memory;
import emustudio.architecture.ArchitectureLoader.PluginInfo;
import emustudio.architecture.drawing.Schema;
import emustudio.main.CommandLineFactory;
import emustudio.main.Main;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.easymock.EasyMock;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class ArchitectureManagerTest {
    
    private static CPU cpuMock;
    private static Memory memoryMock;
    private static Compiler compilerMock;
    private static Device deviceMock;
    
    @BeforeClass
    public static void setUpClass() {
        cpuMock = EasyMock.createNiceMock(CPU.class);
        EasyMock.expect(cpuMock.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE).anyTimes();
        
        memoryMock = EasyMock.createNiceMock(Memory.class);
        EasyMock.expect(memoryMock.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE).anyTimes();
        
        compilerMock = EasyMock.createNiceMock(Compiler.class);
        EasyMock.expect(compilerMock.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE).anyTimes();

        deviceMock = EasyMock.createNiceMock(Device.class);
        EasyMock.expect(deviceMock.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE).anyTimes();
        
        Main.commandLine = EasyMock.createNiceMock(CommandLineFactory.CommandLine.class);
        EasyMock.expect(Main.commandLine.autoWanted()).andReturn(Boolean.FALSE).anyTimes();
        EasyMock.expect(Main.commandLine.noGUIWanted()).andReturn(Boolean.FALSE).anyTimes();
        
        EasyMock.replay(cpuMock, memoryMock, compilerMock, deviceMock, Main.commandLine);
    }
    
    @AfterClass
    public static void tearDownClass() {
        Main.commandLine = null;
    }
    
    /**
     * Test of getSchema method, of class ArchitectureManager.
     */
    @Test
    public void testGetSchema() throws PluginInitializationException {
        Schema schema = EasyMock.createNiceMock(Schema.class);
        Computer computer = EasyMock.createNiceMock(Computer.class);
        EasyMock.expect(computer.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE);
        EasyMock.expect(computer.getPluginsInfo()).andReturn(new ArrayList<PluginInfo>());
        EasyMock.replay(computer);
        
        ArchitectureManager instance = new ArchitectureManager(computer, new Properties(), schema, null);
        
        assertSame(schema, instance.getSchema());
        EasyMock.verify(computer);
    }

    /**
     * Test of getComputerName method, of class ArchitectureManager.
     */
    @Test
    public void testGetComputerName() throws PluginInitializationException {
        Schema schema = EasyMock.createNiceMock(Schema.class);
        EasyMock.expect(schema.getConfigName()).andReturn("TEST");
        Computer computer = EasyMock.createNiceMock(Computer.class);
        EasyMock.expect(computer.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE);
        EasyMock.expect(computer.getPluginsInfo()).andReturn(new ArrayList<PluginInfo>());
        EasyMock.replay(computer, schema);
        
        ArchitectureManager instance = new ArchitectureManager(computer, new Properties(), schema, null);
        
        assertEquals("TEST", instance.getComputerName());
        EasyMock.verify(computer, schema);
    }

    /**
     * Test of getComputer method, of class ArchitectureManager.
     */
    @Test
    public void testGetComputer() throws PluginInitializationException {
        Schema schema = EasyMock.createNiceMock(Schema.class);
        Computer computer = EasyMock.createNiceMock(Computer.class);
        EasyMock.expect(computer.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE);
        EasyMock.expect(computer.getPluginsInfo()).andReturn(new ArrayList<PluginInfo>());
        EasyMock.replay(computer);
        
        ArchitectureManager instance = new ArchitectureManager(computer, new Properties(), schema, null);
        
        assertSame(computer, instance.getComputer());
        EasyMock.verify(computer);
    }

    /**
     * Test of readSetting method, of class ArchitectureManager.
     */
    @Test
    public void testReadSetting() throws PluginInitializationException {
        Schema schema = EasyMock.createNiceMock(Schema.class);
        Computer computer = EasyMock.createNiceMock(Computer.class);
        EasyMock.expect(computer.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE);
        
        List<PluginInfo> pluginNames = new ArrayList<PluginInfo>();
        PluginInfo cpuInfo = EasyMock.createNiceMock(PluginInfo.class);
        pluginNames.add(cpuInfo);
        cpuInfo.pluginSettingsName = "cpu";
        
        EasyMock.expect(computer.getPluginsInfo()).andReturn(pluginNames).anyTimes();
        EasyMock.replay(cpuInfo, computer);
        
        Properties settings = new Properties();
        settings.setProperty("cpu.test", "true");
        
        ArchitectureManager instance = new ArchitectureManager(computer, settings, schema, null);
        
        assertEquals("true", instance.readSetting(0, "test"));
        EasyMock.verify(cpuInfo, computer);
    }

    /**
     * Test of getDeviceName method, of class ArchitectureManager.
     */
    @Test
    public void testGetDeviceName() throws PluginInitializationException {
        Schema schema = EasyMock.createNiceMock(Schema.class);
        Computer computer = EasyMock.createNiceMock(Computer.class);
        EasyMock.expect(computer.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE);
        EasyMock.expect(computer.getPluginsInfo()).andReturn(new ArrayList<PluginInfo>());
        EasyMock.replay(computer);
        
        Properties settings = new Properties();
        settings.setProperty("device0", "deviceName");
        
        ArchitectureManager instance = new ArchitectureManager(computer, settings, schema, null);
        
        assertEquals("deviceName", instance.getDeviceName(0));
        EasyMock.verify(computer);
    }

    /**
     * Test of getCompilerName method, of class ArchitectureManager.
     */
    @Test
    public void testGetCompilerName() throws PluginInitializationException {
        Schema schema = EasyMock.createNiceMock(Schema.class);
        Computer computer = EasyMock.createNiceMock(Computer.class);
        EasyMock.expect(computer.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE);
        EasyMock.expect(computer.getPluginsInfo()).andReturn(new ArrayList<PluginInfo>());
        EasyMock.replay(computer);
        
        Properties settings = new Properties();
        settings.setProperty("compiler", "compilerName");
        
        ArchitectureManager instance = new ArchitectureManager(computer, settings, schema, null);
        
        assertEquals("compilerName", instance.getCompilerName());
        EasyMock.verify(computer);
    }

    /**
     * Test of getCPUName method, of class ArchitectureManager.
     */
    @Test
    public void testGetCPUName() throws PluginInitializationException {
        Schema schema = EasyMock.createNiceMock(Schema.class);
        Computer computer = EasyMock.createNiceMock(Computer.class);
        EasyMock.expect(computer.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE);
        EasyMock.expect(computer.getPluginsInfo()).andReturn(new ArrayList<PluginInfo>());
        EasyMock.replay(computer);
        
        Properties settings = new Properties();
        settings.setProperty("cpu", "cpuName");
        
        ArchitectureManager instance = new ArchitectureManager(computer, settings, schema, null);
        
        assertEquals("cpuName", instance.getCPUName());
        EasyMock.verify(computer);
    }

    /**
     * Test of getMemoryName method, of class ArchitectureManager.
     */
    @Test
    public void testGetMemoryName() throws PluginInitializationException {
        Schema schema = EasyMock.createNiceMock(Schema.class);
        Computer computer = EasyMock.createNiceMock(Computer.class);
        EasyMock.expect(computer.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE);
        EasyMock.expect(computer.getPluginsInfo()).andReturn(new ArrayList<PluginInfo>());
        EasyMock.replay(computer);
        
        Properties settings = new Properties();
        settings.setProperty("memory", "memoryName");
        
        ArchitectureManager instance = new ArchitectureManager(computer, settings, schema, null);
        
        assertEquals("memoryName", instance.getMemoryName());
        EasyMock.verify(computer);
    }

    /**
     * Test of writeSetting method, of class ArchitectureManager.
     */
    @Test
    public void testWriteSetting() throws PluginInitializationException {
        Schema schema = EasyMock.createNiceMock(Schema.class);
        Computer computer = EasyMock.createNiceMock(Computer.class);
        EasyMock.expect(computer.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE);
        
        List<PluginInfo> pluginNames = new ArrayList<PluginInfo>();
        PluginInfo cpuInfo = EasyMock.createNiceMock(PluginInfo.class);
        pluginNames.add(cpuInfo);
        cpuInfo.pluginSettingsName = "cpu";
        
        EasyMock.expect(computer.getPluginsInfo()).andReturn(pluginNames).anyTimes();
        EasyMock.replay(computer);
        
        ArchitectureManager instance = new ArchitectureManager(computer, new Properties(), schema, null);
        assertTrue(instance.writeSetting(0, "test", "true"));
        
        assertEquals("true", instance.readSetting(0, "test"));
        EasyMock.verify(computer);
    }

    /**
     * Test of removeSetting method, of class ArchitectureManager.
     */
    @Test
    public void testRemoveSetting() throws PluginInitializationException {
        Schema schema = EasyMock.createNiceMock(Schema.class);
        Computer computer = EasyMock.createNiceMock(Computer.class);
        EasyMock.expect(computer.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE);
        
        List<PluginInfo> pluginNames = new ArrayList<PluginInfo>();
        PluginInfo cpuInfo = EasyMock.createNiceMock(PluginInfo.class);
        pluginNames.add(cpuInfo);
        cpuInfo.pluginSettingsName = "cpu";
        
        EasyMock.expect(computer.getPluginsInfo()).andReturn(pluginNames).anyTimes();
        EasyMock.replay(computer);
        
        Properties settings = new Properties();
        settings.setProperty("cpu.test", "true");
        
        ArchitectureManager instance = new ArchitectureManager(computer, settings, schema, null);
        assertEquals("true", instance.readSetting(0, "test"));
        assertTrue(instance.removeSetting(0, "test"));
        assertNull(instance.readSetting(0, "test"));
        
        EasyMock.verify(computer);
    }

    /**
     * Test of writeSetting method, of class ArchitectureManager.
     */
    @Test
    public void testWriteSettingToAll() throws PluginInitializationException {
        Schema schema = EasyMock.createNiceMock(Schema.class);
        Computer computer = EasyMock.createNiceMock(Computer.class);
        EasyMock.expect(computer.initialize(EasyMock.anyObject(ArchitectureManager.class))).andReturn(Boolean.TRUE);
        
        List<PluginInfo> pluginNames = new ArrayList<PluginInfo>();
        PluginInfo cpuInfo = EasyMock.createNiceMock(PluginInfo.class);
        PluginInfo compilerInfo = EasyMock.createNiceMock(PluginInfo.class);
        pluginNames.add(cpuInfo);
        pluginNames.add(compilerInfo);
        cpuInfo.pluginSettingsName = "cpu";
        cpuInfo.pluginId = 0;
        compilerInfo.pluginSettingsName = "compiler";
        compilerInfo.pluginId = 1;
        
        EasyMock.expect(computer.getPluginsInfo()).andReturn(pluginNames).anyTimes();
        EasyMock.replay(computer);
        
        ArchitectureManager instance = new ArchitectureManager(computer, new Properties(), schema, null);
        assertTrue(instance.writeSetting("test", "true"));
        
        assertEquals("true", instance.readSetting(0, "test"));
        assertEquals("true", instance.readSetting(1, "test"));
        EasyMock.verify(computer);
    }
}
