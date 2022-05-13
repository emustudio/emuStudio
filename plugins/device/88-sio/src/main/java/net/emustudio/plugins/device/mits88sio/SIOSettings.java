/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

import net.emustudio.emulib.runtime.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.PluginSettings;
import net.jcip.annotations.ThreadSafe;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@ThreadSafe
public class SIOSettings {
    static final String STATUS_PORT_NUMBER = "statusPortNumber";
    static final String DATA_PORT_NUMBER = "dataPortNumber";

    private final PluginSettings settings;
    private final boolean guiNotSupported;

    private final List<Integer> statusPorts = new CopyOnWriteArrayList<>();
    private final List<Integer> dataPorts = new CopyOnWriteArrayList<>();

    private final List<ChangedObserver> observers = new CopyOnWriteArrayList<>();

    public SIOSettings(PluginSettings settings) {
        this.settings = Objects.requireNonNull(settings);
        this.guiNotSupported = settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
    }

    interface ChangedObserver {
        void settingsChanged();
    }

    void addChangedObserver(ChangedObserver observer) {
        observers.add(observer);
    }

    void removeChangedObserver(ChangedObserver observer) {
        observers.remove(observer);
    }

    boolean isGuiNotSupported() {
        return guiNotSupported;
    }

    public Collection<Integer> getStatusPorts() {
        return Collections.unmodifiableCollection(statusPorts);
    }

    public List<Integer> getDefaultStatusPorts() {
        return List.of(3, 16, 20, 22, 24);
    }

    public List<Integer> getDefaultDataPorts() {
        return List.of(2, 17, 21, 23, 25);
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

    public void write() throws CannotUpdateSettingException {
        writePorts(statusPorts, STATUS_PORT_NUMBER);
        writePorts(dataPorts, DATA_PORT_NUMBER);
    }

    void read() {
        readPorts(statusPorts, STATUS_PORT_NUMBER);
        readPorts(dataPorts, DATA_PORT_NUMBER);
        notifyObservers();
    }

    private void readPorts(List<Integer> ports, String baseName) {
        ports.clear();
        int i = 0;

        do {
            Optional<Integer> portOpt = settings.getInt(baseName + i);
            if (portOpt.isPresent()) {
                ports.add(portOpt.get());
            } else {
                break;
            }
            i++;
        } while(true);
    }

    private void writePorts(Collection<Integer> ports, String baseName) throws CannotUpdateSettingException {
        int i = 0;
        for (int port : ports) {
            settings.setInt(baseName + i, port);
            i++;
        }
        while (settings.contains(baseName + i)) {
            settings.remove(baseName + i);
            i++;
        }
    }

    private void notifyObservers() {
        observers.forEach(ChangedObserver::settingsChanged);
    }
}
