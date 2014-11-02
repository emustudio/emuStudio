/*
 * SIOSettings.java
 *
 * Copyright (C) 2013 Peter Jakubčo
 * KISS, YAGNI, DRY
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
import emulib.runtime.LoggerFactory;
import emulib.runtime.interfaces.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SIOSettings {
    private final static Logger LOGGER = LoggerFactory.getLogger(SIOSettings.class);

    private final static String STATUS_PORT_NUMBER = "statusPortNumber";
    private final static String DATA_PORT_NUMBER = "dataPortNumber";

    public static final int DEFAULT_STATUS_PORT_NUMBER = 0x10;
    public static final int DEFAULT_DATA_PORT_NUMBER = 0x11;

    private final long pluginID;
    private SettingsManager settingsManager;

    private boolean emuStudioNoGUI = false;
    private boolean emuStudioAuto = false;

    private int statusPortNumber = DEFAULT_STATUS_PORT_NUMBER;
    private int dataPortNumber = DEFAULT_DATA_PORT_NUMBER;

    private final List<ChangedObserver> observers = new ArrayList<>();
    private final ReadWriteLock settingsLock = new ReentrantReadWriteLock();

    public interface ChangedObserver {
        void settingsChanged();
    }

    public SIOSettings(long pluginID) {
        this.pluginID = pluginID;
    }

    public void addChangedObserver(ChangedObserver observer) {
        observers.add(observer);
    }

    public void removeChangedObserver(ChangedObserver observer) {
        observers.remove(observer);
    }

    public void setSettingsManager(SettingsManager settingsManager) {
        settingsLock.writeLock().lock();
        try {
            this.settingsManager = settingsManager;
        } finally {
            settingsLock.writeLock().unlock();
        }
    }

    private void notifyObservers() {
        for (ChangedObserver observer : observers) {
            observer.settingsChanged();
        }
    }

    public boolean isNoGUI() {
        settingsLock.readLock().lock();
        try {
            return emuStudioNoGUI;
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public int getStatusPortNumber() {
        settingsLock.readLock().lock();
        try {
            return statusPortNumber;
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public void setStatusPortNumber(int statusPortNumber) {
        settingsLock.writeLock().lock();
        try {
            this.statusPortNumber = statusPortNumber;
        } finally {
            settingsLock.writeLock().unlock();
        }
        notifyObservers();
    }

    public int getDataPortNumber() {
        settingsLock.readLock().lock();
        try {
            return dataPortNumber;
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public void setDataPortNumber(int dataPortNumber) {
        settingsLock.writeLock().lock();
        try {
            this.dataPortNumber = dataPortNumber;
        } finally {
            settingsLock.writeLock().unlock();
        }
        notifyObservers();
    }

    public boolean isAuto() {
        settingsLock.readLock().lock();
        try {
            return emuStudioAuto;
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public void write() {
        settingsLock.readLock().lock();
        try {
            if (settingsManager == null) {
                return;
            }
            settingsManager.writeSetting(pluginID, SettingsManager.NO_GUI, String.valueOf(emuStudioNoGUI));
            settingsManager.writeSetting(pluginID, SettingsManager.AUTO, String.valueOf(emuStudioAuto));
            settingsManager.writeSetting(pluginID, STATUS_PORT_NUMBER, String.valueOf(statusPortNumber));
            settingsManager.writeSetting(pluginID, DATA_PORT_NUMBER, String.valueOf(dataPortNumber));
        } finally {
            settingsLock.readLock().unlock();
        }
    }

    public void read() {
        settingsLock.writeLock().lock();
        try {
            if (settingsManager == null) {
                return;
            }
            emuStudioNoGUI = Boolean.parseBoolean(settingsManager.readSetting(pluginID, SettingsManager.NO_GUI));
            emuStudioAuto = Boolean.parseBoolean(settingsManager.readSetting(pluginID, SettingsManager.AUTO));
            String tmp = settingsManager.readSetting(pluginID, STATUS_PORT_NUMBER);
            if (tmp != null) {
                try {
                    statusPortNumber = Integer.decode(tmp);
                } catch (NumberFormatException e) {
                    LOGGER.error("Could not read setting: status port number", e);
                }
            }
            tmp = settingsManager.readSetting(pluginID, DATA_PORT_NUMBER);
            if (tmp != null) {
                try {
                    dataPortNumber = Integer.decode(tmp);
                } catch (NumberFormatException e) {
                    LOGGER.error("Could not read setting: data port number", e);
                }
            }
        } finally {
            settingsLock.writeLock().unlock();
        }
        notifyObservers();
    }


}
