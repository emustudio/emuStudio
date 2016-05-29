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
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@ThreadSafe
public class SIOSettings {
    private final static Logger LOGGER = LoggerFactory.getLogger(SIOSettings.class);

    static final String STATUS_PORT_NUMBER = "statusPortNumber";
    static final String DATA_PORT_NUMBER = "dataPortNumber";
    public static final int DEFAULT_STATUS_PORT_NUMBER = 0x10;
    public static final int DEFAULT_DATA_PORT_NUMBER = 0x11;

    private final long pluginID;
    private volatile SettingsManager settingsManager;

    private volatile boolean emuStudioNoGUI = false;

    private volatile int statusPortNumber = DEFAULT_STATUS_PORT_NUMBER;
    private volatile int dataPortNumber = DEFAULT_DATA_PORT_NUMBER;

    private final List<ChangedObserver> observers = new CopyOnWriteArrayList<>();

    interface ChangedObserver {
        void settingsChanged();
    }

    SIOSettings(long pluginID) {
        this.pluginID = pluginID;
    }

    void addChangedObserver(ChangedObserver observer) {
        observers.add(observer);
    }

    void removeChangedObserver(ChangedObserver observer) {
        observers.remove(observer);
    }

    void setSettingsManager(SettingsManager settingsManager) {
        this.settingsManager = Objects.requireNonNull(settingsManager);
    }

    private void notifyObservers() {
        for (ChangedObserver observer : observers) {
            observer.settingsChanged();
        }
    }

    boolean isNoGUI() {
        return emuStudioNoGUI;
    }

    public int getStatusPortNumber() {
        return statusPortNumber;
    }

    public void setStatusPortNumber(int statusPortNumber) {
        this.statusPortNumber = statusPortNumber;
        notifyObservers();
    }

    public int getDataPortNumber() {
        return dataPortNumber;
    }

    public void setDataPortNumber(int dataPortNumber) {
        this.dataPortNumber = dataPortNumber;
        notifyObservers();
    }

    public void write() {
        SettingsManager tmpManager = settingsManager;
        if (tmpManager != null) {
            synchronized (this) {
                tmpManager.writeSetting(pluginID, STATUS_PORT_NUMBER, String.valueOf(statusPortNumber));
                tmpManager.writeSetting(pluginID, DATA_PORT_NUMBER, String.valueOf(dataPortNumber));
            }
        }
    }

    void read() {
        SettingsManager tmpManager = settingsManager;
        if (tmpManager != null) {
            synchronized (this) {
                emuStudioNoGUI = Boolean.parseBoolean(tmpManager.readSetting(pluginID, SettingsManager.NO_GUI));
                String tmp = tmpManager.readSetting(pluginID, STATUS_PORT_NUMBER);
                if (tmp != null) {
                    try {
                        statusPortNumber = Integer.decode(tmp);
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not read setting: status port number", e);
                    }
                }
                tmp = tmpManager.readSetting(pluginID, DATA_PORT_NUMBER);
                if (tmp != null) {
                    try {
                        dataPortNumber = Integer.decode(tmp);
                    } catch (NumberFormatException e) {
                        LOGGER.error("Could not read setting: data port number", e);
                    }
                }
            }
        }
        notifyObservers();
    }


}
