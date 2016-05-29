/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package net.sf.emustudio.devices.mits88sio.impl;

import emulib.emustudio.SettingsManager;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

public class SIOSettingsTest {
    private static final int PLUGIN_ID = 1122;

    @Test
    public void testObserverIsCalledOnRead() throws Exception {
        SIOSettings.ChangedObserver observer = createMock(SIOSettings.ChangedObserver.class);
        observer.settingsChanged();
        expectLastCall().once();
        replay(observer);

        SIOSettings settings = new SIOSettings(PLUGIN_ID);
        settings.addChangedObserver(observer);
        settings.read();

        verify(observer);
    }

    @Test
    public void testObserverIsCalledOnSetStatusPort() throws Exception {
        SIOSettings.ChangedObserver observer = createMock(SIOSettings.ChangedObserver.class);
        observer.settingsChanged();
        expectLastCall().once();
        replay(observer);

        SIOSettings settings = new SIOSettings(PLUGIN_ID);
        settings.addChangedObserver(observer);
        settings.setStatusPortNumber(0);

        verify(observer);
    }

    @Test
    public void testObserverIsCalledOnSetDataPort() throws Exception {
        SIOSettings.ChangedObserver observer = createMock(SIOSettings.ChangedObserver.class);
        observer.settingsChanged();
        expectLastCall().once();
        replay(observer);

        SIOSettings settings = new SIOSettings(PLUGIN_ID);
        settings.addChangedObserver(observer);
        settings.setDataPortNumber(0);

        verify(observer);
    }

    @Test
    public void testValuesAreCorrectAfterRead() throws Exception {
        SettingsManager manager = createMock(SettingsManager.class);
        expect(manager.readSetting(PLUGIN_ID, SettingsManager.NO_GUI)).andReturn("true").once();
        expect(manager.readSetting(PLUGIN_ID, SIOSettings.STATUS_PORT_NUMBER)).andReturn("5").once();
        expect(manager.readSetting(PLUGIN_ID, SIOSettings.DATA_PORT_NUMBER)).andReturn("10").once();
        replay(manager);

        SIOSettings settings = new SIOSettings(PLUGIN_ID);
        settings.setSettingsManager(manager);
        settings.read();

        assertEquals(true, settings.isNoGUI());
        assertEquals(5, settings.getStatusPortNumber());
        assertEquals(10, settings.getDataPortNumber());

        verify(manager);
    }

    @Test
    public void testCorrectValuesAreWrittenAfterWrite() throws Exception {
        SettingsManager manager = createMock(SettingsManager.class);
        expect(manager.writeSetting(PLUGIN_ID, SIOSettings.STATUS_PORT_NUMBER, "5")).andReturn(true).once();
        expect(manager.writeSetting(PLUGIN_ID, SIOSettings.DATA_PORT_NUMBER, "10")).andReturn(true).once();
        replay(manager);

        SIOSettings settings = new SIOSettings(PLUGIN_ID);
        settings.setSettingsManager(manager);
        settings.setDataPortNumber(10);
        settings.setStatusPortNumber(5);
        settings.write();

        verify(manager);
    }

    @Test
    public void testIfPortsCannotBeParsedDefaultsAreAssigned() throws Exception {
        SettingsManager manager = createMock(SettingsManager.class);
        expect(manager.readSetting(PLUGIN_ID, SettingsManager.NO_GUI)).andReturn("true").once();
        expect(manager.readSetting(PLUGIN_ID, SIOSettings.STATUS_PORT_NUMBER)).andReturn("abc").once();
        expect(manager.readSetting(PLUGIN_ID, SIOSettings.DATA_PORT_NUMBER)).andReturn("def").once();
        replay(manager);

        SIOSettings settings = new SIOSettings(PLUGIN_ID);
        settings.setSettingsManager(manager);
        settings.read();

        assertEquals(true, settings.isNoGUI());
        assertEquals(SIOSettings.DEFAULT_STATUS_PORT_NUMBER, settings.getStatusPortNumber());
        assertEquals(SIOSettings.DEFAULT_DATA_PORT_NUMBER, settings.getDataPortNumber());

        verify(manager);
    }
}
