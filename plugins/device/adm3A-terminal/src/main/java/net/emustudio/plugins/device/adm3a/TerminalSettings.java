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
package net.emustudio.plugins.device.adm3a;

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TerminalSettings {
    public final static String DEFAULT_INPUT_FILE_NAME = "adm3A-terminal.in";
    public final static String DEFAULT_OUTPUT_FILE_NAME = "adm3A-terminal.out";
    private final static Logger LOGGER = LoggerFactory.getLogger(TerminalSettings.class);
    private final static String HALF_DUPLEX = "halfDuplex";
    private final static String ALWAYS_ON_TOP = "alwaysOnTop";
    private final static String INPUT_FILE_NAME = "inputFileName";
    private final static String OUTPUT_FILE_NAME = "outputFileName";
    private final static String INPUT_READ_DELAY = "inputReadDelay";
    private final static String DEVICE_INDEX = "deviceIndex";
    private final static String FONT = "font";
    private final Dialogs dialogs;
    private final PluginSettings settings;
    private final boolean guiSupported;
    private final List<ChangedObserver> observers = new ArrayList<>();
    private boolean halfDuplex = false;
    private boolean alwaysOnTop = false;
    private volatile Path inputPath = Path.of(DEFAULT_INPUT_FILE_NAME);
    private volatile Path outputPath = Path.of(DEFAULT_OUTPUT_FILE_NAME);
    private int inputReadDelay = 0;
    private int deviceIndex = 0;
    private TerminalFont font = TerminalFont.ORIGINAL;

    TerminalSettings(PluginSettings settings, Dialogs dialogs) {
        this.dialogs = Objects.requireNonNull(dialogs);
        this.settings = Objects.requireNonNull(settings);

        guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        readSettings();
    }

    public void addChangedObserver(ChangedObserver observer) {
        observers.add(observer);
    }

    public void removeChangedObserver(ChangedObserver observer) {
        observers.remove(observer);
    }

    public boolean isGuiSupported() {
        return guiSupported;
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

    public Path getInputPath() {
        return inputPath;
    }

    public void setInputPath(Path inputPath) throws IOException {
        this.inputPath = inputPath;
        notifyObservers();
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(Path outputFileName) throws IOException {
        this.outputPath = outputFileName;
        notifyObservers();
    }

    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
        notifyObserversAndIgnoreError();
    }

    public int getDeviceIndex() {
        return deviceIndex;
    }

    public void setDeviceIndex(int deviceIndex) {
        this.deviceIndex = deviceIndex;
    }

    public TerminalFont getFont() {
        return font;
    }

    public void setFont(TerminalFont font) {
        this.font = Objects.requireNonNull(font);
    }

    public void write() {
        try {
            settings.setInt(INPUT_READ_DELAY, inputReadDelay);
            settings.setBoolean(HALF_DUPLEX, halfDuplex);
            settings.setBoolean(ALWAYS_ON_TOP, alwaysOnTop);
            settings.setString(OUTPUT_FILE_NAME, outputPath.toString());
            settings.setString(INPUT_FILE_NAME, inputPath.toString());
            settings.setInt(DEVICE_INDEX, deviceIndex);
            settings.setString(FONT, font.name);
        } catch (CannotUpdateSettingException e) {
            LOGGER.error("Could not update settings", e);
            dialogs.showError("Could not save settings. Please see log file for details.", "ADM 3A");
        } finally {
            notifyObserversAndIgnoreError();
        }
    }

    private void readSettings() {
        halfDuplex = settings.getBoolean(HALF_DUPLEX, false);
        alwaysOnTop = settings.getBoolean(ALWAYS_ON_TOP, false);
        inputPath = Path.of(settings.getString(INPUT_FILE_NAME, DEFAULT_INPUT_FILE_NAME));
        outputPath = Path.of(settings.getString(OUTPUT_FILE_NAME, DEFAULT_OUTPUT_FILE_NAME));
        deviceIndex = settings.getInt(DEVICE_INDEX, 0);
        try {
            inputReadDelay = settings.getInt(INPUT_READ_DELAY, 0);
        } catch (NumberFormatException e) {
            inputReadDelay = 0;
            LOGGER.error(
                    "Could not read '" + INPUT_READ_DELAY + "' setting. Using default value ({})", inputReadDelay, e
            );
        }
        font = TerminalFont.valueOf(settings.getString(FONT, TerminalFont.ORIGINAL.name).toUpperCase());

        if (inputPath.toString().equals(outputPath.toString())) {
            LOGGER.error("ADM-3A settings: Input path is not allowed to be equal to the output path. Setting to default.");
            inputPath = Path.of(DEFAULT_INPUT_FILE_NAME);
            outputPath = Path.of(DEFAULT_OUTPUT_FILE_NAME);
        }

        notifyObserversAndIgnoreError();
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

    public enum TerminalFont {
        ORIGINAL("original"),
        MODERN("modern");

        public final String name;

        TerminalFont(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public static TerminalFont valueOf(int index) {
            if (index == 0) {
                return ORIGINAL;
            } else if (index == 1) {
                return MODERN;
            } else {
                throw new RuntimeException("Unknown font index: " + index);
            }
        }
    }

    public interface ChangedObserver {
        void settingsChanged() throws IOException;
    }
}
