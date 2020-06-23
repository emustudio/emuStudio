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
package net.emustudio.plugins.device.mits88sio;

import net.emustudio.emulib.runtime.PluginSettings;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SIOSettingsTest {

    @Test
    public void testObserverIsCalledOnRead() {
        SIOSettings.ChangedObserver observer = createMock(SIOSettings.ChangedObserver.class);
        observer.settingsChanged();
        expectLastCall().once();
        replay(observer);

        SIOSettings settings = new SIOSettings(PluginSettings.UNAVAILABLE);
        settings.addChangedObserver(observer);
        settings.read();

        verify(observer);
    }

    @Test
    public void testObserverIsCalledOnSetStatusPort() {
        SIOSettings.ChangedObserver observer = createMock(SIOSettings.ChangedObserver.class);
        observer.settingsChanged();
        expectLastCall().once();
        replay(observer);

        SIOSettings settings = new SIOSettings(PluginSettings.UNAVAILABLE);
        settings.addChangedObserver(observer);
        settings.setStatusPorts(Collections.singletonList(0));

        verify(observer);
    }

    @Test
    public void testObserverIsCalledOnSetDataPort() {
        SIOSettings.ChangedObserver observer = createMock(SIOSettings.ChangedObserver.class);
        observer.settingsChanged();
        expectLastCall().once();
        replay(observer);

        SIOSettings settings = new SIOSettings(PluginSettings.UNAVAILABLE);
        settings.addChangedObserver(observer);
        settings.setDataPorts(Collections.singletonList(0));

        verify(observer);
    }

    @Test
    public void testValuesAreCorrectAfterRead() {
        PluginSettings pluginSettings = createMock(PluginSettings.class);
        expect(pluginSettings.getBoolean(eq(PluginSettings.EMUSTUDIO_NO_GUI), eq(false))).andReturn(true).once();
        expect(pluginSettings.getInt(SIOSettings.STATUS_PORT_NUMBER + "0")).andReturn(Optional.of(5)).once();
        expect(pluginSettings.getInt(SIOSettings.STATUS_PORT_NUMBER + "1")).andReturn(Optional.empty()).once();
        expect(pluginSettings.getInt(SIOSettings.DATA_PORT_NUMBER + "0")).andReturn(Optional.of(10)).once();
        expect(pluginSettings.getInt(SIOSettings.DATA_PORT_NUMBER + "1")).andReturn(Optional.empty()).once();
        replay(pluginSettings);

        SIOSettings settings = new SIOSettings(pluginSettings);
        settings.read();

        assertTrue(settings.isGuiNotSupported());
        assertEquals(5, (int) settings.getStatusPorts().iterator().next());
        assertEquals(10, (int) settings.getDataPorts().iterator().next());

        verify(pluginSettings);
    }

    @Test
    public void testCorrectValuesAreWrittenAfterWrite() throws Exception {
        PluginSettings pluginSettings = createMock(PluginSettings.class);
        expect(pluginSettings.getBoolean(eq(PluginSettings.EMUSTUDIO_NO_GUI), eq(false))).andReturn(false).anyTimes();
        pluginSettings.setInt(eq(SIOSettings.STATUS_PORT_NUMBER + "0"), eq(5));
        expectLastCall().once();
        pluginSettings.setInt(eq(SIOSettings.DATA_PORT_NUMBER + "0"), eq(10));
        expectLastCall().once();
        expect(pluginSettings.contains("statusPortNumber1")).andReturn(false).once();
        expect(pluginSettings.contains("dataPortNumber1")).andReturn(false).once();
        replay(pluginSettings);

        SIOSettings settings = new SIOSettings(pluginSettings);
        settings.setDataPorts(Collections.singletonList(10));
        settings.setStatusPorts(Collections.singletonList(5));
        settings.write();

        verify(pluginSettings);
    }

    @Test
    public void testRemovedPortsAreReallyRemoved() throws Exception {
        PluginSettings pluginSettings = createMock(PluginSettings.class);
        expect(pluginSettings.getBoolean(eq(PluginSettings.EMUSTUDIO_NO_GUI), eq(false))).andReturn(false).anyTimes();

        pluginSettings.setInt(eq(SIOSettings.STATUS_PORT_NUMBER + "0"), eq(10));
        expectLastCall().once();
        expect(pluginSettings.contains("statusPortNumber0")).andReturn(true).once();
        expect(pluginSettings.contains("statusPortNumber1")).andReturn(false).times(2);
        expect(pluginSettings.contains("dataPortNumber0")).andReturn(false).times(2);

        pluginSettings.remove("statusPortNumber0");
        expectLastCall().once();

        replay(pluginSettings);

        SIOSettings settings = new SIOSettings(pluginSettings);
        settings.setStatusPorts(Collections.singletonList(10));
        settings.write();

        settings.setStatusPorts(Collections.emptyList());
        settings.write();

        verify(pluginSettings);
    }
}
