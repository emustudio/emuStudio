/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100;

import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NotThreadSafe
public class TerminalSettings {
    private final static Logger LOGGER = LoggerFactory.getLogger(TerminalSettings.class);

    public final static String DEFAULT_INPUT_FILE_NAME = "vt100-terminal.in";
    public final static String DEFAULT_OUTPUT_FILE_NAME = "vt100-terminal.out";
    public final static int DEFAULT_COLUMNS = 80;
    public final static int DEFAULT_ROWS = 24;
    public final static int DEFAULT_INPUT_READ_DELAY_MILLIS = 0;

    private final static String KEY_INPUT_FILE_NAME = "inputFileName";
    private final static String KEY_OUTPUT_FILE_NAME = "outputFileName";
    private final static String KEY_INPUT_READ_DELAY_MILLIS = "inputReadDelayMillis";
    private final static String KEY_COLUMNS = "columns";
    private final static String KEY_ROWS = "rows";

    private final Dialogs dialogs;
    private final PluginSettings settings;
    private final boolean guiSupported;

    private final List<SizeChangedObserver> sizeChangedObservers = new ArrayList<>();

    private volatile Path inputPath = Path.of(DEFAULT_INPUT_FILE_NAME);
    private volatile Path outputPath = Path.of(DEFAULT_OUTPUT_FILE_NAME);
    private int inputReadDelayMillis = DEFAULT_INPUT_READ_DELAY_MILLIS;
    private int columns = DEFAULT_COLUMNS;
    private int rows = DEFAULT_ROWS;

    public interface SizeChangedObserver {
        void sizeChanged(int columns, int rows);
    }

    TerminalSettings(PluginSettings settings, Dialogs dialogs) {
        this.dialogs = Objects.requireNonNull(dialogs);
        this.settings = Objects.requireNonNull(settings);

        guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        readSettings();
    }

    public void addSizeChangedObserver(SizeChangedObserver observer) {
        this.sizeChangedObservers.add(observer);
    }

    public void destroy() {
        sizeChangedObservers.clear();
    }

    public boolean isGuiSupported() {
        return guiSupported;
    }

    public int getInputReadDelayMillis() {
        return inputReadDelayMillis;
    }

    public void setInputReadDelayMillis(int inputReadDelayMillis) {
        this.inputReadDelayMillis = inputReadDelayMillis;
    }

    public Path getInputPath() {
        return inputPath;
    }

    public void setInputPath(Path inputPath) {
        this.inputPath = inputPath;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(Path outputFileName) {
        this.outputPath = outputFileName;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public void setSize(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
        sizeChangedObservers.forEach(a -> a.sizeChanged(columns, rows));
    }

    public void write() {
        try {
            settings.setInt(KEY_INPUT_READ_DELAY_MILLIS, inputReadDelayMillis);
            settings.setString(KEY_OUTPUT_FILE_NAME, outputPath.toString());
            settings.setString(KEY_INPUT_FILE_NAME, inputPath.toString());
            settings.setInt(KEY_COLUMNS, columns);
            settings.setInt(KEY_ROWS, rows);
        } catch (CannotUpdateSettingException e) {
            LOGGER.error("Could not update settings", e);
            dialogs.showError("Could not save settings. Please see log file for details.", "VT100 Terminal");
        }
    }

    private void readSettings() {
        this.inputPath = Path.of(settings.getString(KEY_INPUT_FILE_NAME, DEFAULT_INPUT_FILE_NAME));
        this.outputPath = Path.of(settings.getString(KEY_OUTPUT_FILE_NAME, DEFAULT_OUTPUT_FILE_NAME));
        try {
            this.inputReadDelayMillis = settings.getInt(KEY_INPUT_READ_DELAY_MILLIS, DEFAULT_INPUT_READ_DELAY_MILLIS);
        } catch (NumberFormatException e) {
            this.inputReadDelayMillis = DEFAULT_INPUT_READ_DELAY_MILLIS;
            LOGGER.error(
                    "Could not read '" + KEY_INPUT_READ_DELAY_MILLIS + "' setting. Using default value ({})", inputReadDelayMillis, e
            );
        }
        try {
            this.columns = settings.getInt(KEY_COLUMNS, DEFAULT_COLUMNS);
        } catch (NumberFormatException e) {
            this.columns = DEFAULT_COLUMNS;
            LOGGER.error(
                    "Could not read '" + KEY_COLUMNS + "' setting. Using default value ({})", columns, e
            );
        }
        try {
            this.rows = settings.getInt(KEY_ROWS, DEFAULT_ROWS);
        } catch (NumberFormatException e) {
            this.rows = DEFAULT_ROWS;
            LOGGER.error(
                    "Could not read '" + KEY_ROWS + "' setting. Using default value ({})", rows, e
            );
        }

        if (inputPath.toString().equals(outputPath.toString())) {
            LOGGER.error("VT100 Terminal settings: Input path is not allowed to be equal to the output path. Setting to default.");
            this.inputPath = Path.of(DEFAULT_INPUT_FILE_NAME);
            this.outputPath = Path.of(DEFAULT_OUTPUT_FILE_NAME);
        }
    }
}
