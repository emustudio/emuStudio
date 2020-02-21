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
package net.sf.emustudio.devices.mits88sio.impl;

import emulib.emustudio.SettingsManager;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@ThreadSafe
public class SIOSettings {
    private final static Logger LOGGER = LoggerFactory.getLogger(SIOSettings.class);

    static final String STATUS_PORT_NUMBER = "statusPortNumber";
    static final String DATA_PORT_NUMBER = "dataPortNumber";

    private final long pluginID;
    private volatile SettingsManager settingsManager;

    private volatile boolean emuStudioNoGUI = false;

    private final List<Integer> statusPorts = new CopyOnWriteArrayList<>();
    private final List<Integer> dataPorts = new CopyOnWriteArrayList<>();

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
        observers.forEach(ChangedObserver::settingsChanged);
    }

    boolean isNoGUI() {
        return emuStudioNoGUI;
    }

    public Collection<Integer> getStatusPorts() {
        return Collections.unmodifiableCollection(statusPorts);
    }

    public void setStatusPorts(Collection<Integer> statusPorts) {
        this.statusPorts.clear();
        this.statusPorts.addAll(statusPorts);
        notifyObservers();
    }

    public Collection<Integer> getDataPorts() {
        return Collections.unmodifiableCollection(dataPorts);
    }

    public void setDataPorts(Collection<Integer> dataPorts) {
        this.dataPorts.clear();
        this.dataPorts.addAll(dataPorts);
        notifyObservers();
    }

    public void write() {
        SettingsManager tmpManager = settingsManager;
        if (tmpManager != null) {
            synchronized (this) {
                writePorts(statusPorts, STATUS_PORT_NUMBER);
                writePorts(dataPorts, DATA_PORT_NUMBER);
            }
        }
    }

    private void writePorts(Collection<Integer> ports, String baseName) {
        SettingsManager tmpManager = settingsManager;

        int i = 0;
        for (int port : ports) {
            tmpManager.writeSetting(pluginID, baseName + i, String.valueOf(port));
            i++;
        }
    }

    void read() {
        SettingsManager tmpManager = settingsManager;
        if (tmpManager != null) {
            synchronized (this) {
                emuStudioNoGUI = Boolean.parseBoolean(tmpManager.readSetting(pluginID, SettingsManager.NO_GUI));
                readPorts(statusPorts, STATUS_PORT_NUMBER);
                readPorts(dataPorts, DATA_PORT_NUMBER);
            }
        }
        notifyObservers();
    }

    private void readPorts(List<Integer> ports, String baseName) {
        SettingsManager tmpManager = settingsManager;

        ports.clear();
        int i = 0;
        String tmp;
        do {
            tmp = tmpManager.readSetting(pluginID, baseName + i);
            if (tmp != null) {
                try {
                    ports.add(Integer.decode(tmp));
                } catch (NumberFormatException e) {
                    LOGGER.error("Could not parse status port number: {}", tmp, e);
                }
            }
            i++;
        } while (tmp != null);
    }

}
