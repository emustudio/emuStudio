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
package net.sf.emustudio.devices.adm3a.impl;

import emulib.emustudio.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TerminalSettings {
    private final static Logger LOGGER = LoggerFactory.getLogger(TerminalSettings.class);

    private final static String DEFAULT_INPUT_FILE_NAME = "adm3A-terminal.in";
    private final static String DEFAULT_OUTPUT_FILE_NAME = "adm3A-terminal.out";
    private final static String ANTI_ALIASING = "antiAliasing";
    private final static String HALF_DUPLEX = "halfDuplex";
    private final static String ALWAYS_ON_TOP = "alwaysOnTop";
    private final static String INPUT_FILE_NAME = "inputFileName";
    private final static String OUTPUT_FILE_NAME = "outputFileName";
    private final static String INPUT_READ_DELAY = "inputReadDelay";

    private final long pluginID;

    private volatile SettingsManager settingsManager;
    private volatile boolean emuStudioNoGUI = false;
    private volatile boolean emuStudioAuto = false;
    private volatile boolean halfDuplex = false;
    private volatile boolean antiAliasing = true;
    private volatile boolean alwaysOnTop = false;
    private volatile String inputFileName = DEFAULT_INPUT_FILE_NAME;
    private volatile String outputFileName = DEFAULT_OUTPUT_FILE_NAME;
    private volatile int inputReadDelay = 0;

    private final List<ChangedObserver> observers = new ArrayList<>();

    interface ChangedObserver {
        void settingsChanged() throws IOException;
    }

    TerminalSettings(long pluginID) {
        this.pluginID = pluginID;
    }

    void addChangedObserver(ChangedObserver observer) {
        observers.add(observer);
    }

    void removeChangedObserver(ChangedObserver observer) {
        observers.remove(observer);
    }

    void setSettingsManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    private void notifyObservers() throws IOException {
        for (ChangedObserver observer : observers) {
            observer.settingsChanged();
        }
    }

    private void notifyObserversAndIgnoreError() {
        for (ChangedObserver observer : observers) {
            try {
                observer.settingsChanged();
            } catch (IOException e) {
                LOGGER.error("Observer is not happy about the new settings", e);
            }
        }
    }


    boolean isNoGUI() {
        return emuStudioNoGUI;
    }

    public int getInputReadDelay() {
        return inputReadDelay;
    }

    public void setInputReadDelay(int inputReadDelay) {
        this.inputReadDelay = inputReadDelay;
        notifyObserversAndIgnoreError();
    }

    public boolean isHalfDuplex() {
        return halfDuplex;
    }

    public void setHalfDuplex(boolean halfDuplex) {
        this.halfDuplex = halfDuplex;
        notifyObserversAndIgnoreError();
    }

    public String getInputFileName() {
        return inputFileName;
    }

    public void setInputFileName(String inputFileName) throws IOException {
        this.inputFileName = inputFileName;
        notifyObservers();
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public void setOutputFileName(String outputFileName) throws IOException {
        this.outputFileName = outputFileName;
        notifyObservers();
    }

    public boolean isAntiAliasing() {
        return antiAliasing;
    }

    public void setAntiAliasing(boolean antiAliasing) {
        this.antiAliasing = antiAliasing;
        notifyObserversAndIgnoreError();
    }

    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
        notifyObserversAndIgnoreError();
    }

    public void write() {
        if (settingsManager == null) {
            return;
        }
        settingsManager.writeSetting(pluginID, HALF_DUPLEX, String.valueOf(halfDuplex));
        settingsManager.writeSetting(pluginID, ALWAYS_ON_TOP, String.valueOf(alwaysOnTop));
        settingsManager.writeSetting(pluginID, ANTI_ALIASING, String.valueOf(antiAliasing));
        settingsManager.writeSetting(pluginID, INPUT_FILE_NAME, inputFileName);
        settingsManager.writeSetting(pluginID, OUTPUT_FILE_NAME, outputFileName);
        settingsManager.writeSetting(pluginID, INPUT_READ_DELAY, String.valueOf(inputReadDelay));
    }

    void read() throws IOException {
        if (settingsManager == null) {
            return;
        }
        emuStudioNoGUI = Boolean.parseBoolean(settingsManager.readSetting(pluginID, SettingsManager.NO_GUI));
        emuStudioAuto = Boolean.parseBoolean(settingsManager.readSetting(pluginID, SettingsManager.AUTO));
        halfDuplex = Boolean.parseBoolean(settingsManager.readSetting(pluginID, HALF_DUPLEX));
        alwaysOnTop = Boolean.parseBoolean(settingsManager.readSetting(pluginID, ALWAYS_ON_TOP));
        antiAliasing = Boolean.parseBoolean(settingsManager.readSetting(pluginID, ANTI_ALIASING));
        inputFileName = settingsManager.readSetting(pluginID, INPUT_FILE_NAME);
        if (inputFileName == null) {
            inputFileName = DEFAULT_INPUT_FILE_NAME;
        }
        outputFileName = settingsManager.readSetting(pluginID, OUTPUT_FILE_NAME);
        if (outputFileName == null) {
            outputFileName = DEFAULT_OUTPUT_FILE_NAME;
        }
        String tmp = settingsManager.readSetting(pluginID, INPUT_READ_DELAY);
        if (tmp != null) {
            try {
                inputReadDelay = Integer.decode(tmp);
            } catch (NumberFormatException e) {
                LOGGER.error("Could not read setting: input read delay for terminal. Using default value ({})",
                    inputReadDelay, e);
            }
        }
        notifyObservers();
    }

}
