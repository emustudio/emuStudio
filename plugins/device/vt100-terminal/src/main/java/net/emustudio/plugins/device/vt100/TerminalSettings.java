package net.emustudio.plugins.device.vt100;

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
    private final static Logger LOGGER = LoggerFactory.getLogger(TerminalSettings.class);

    public final static String DEFAULT_INPUT_FILE_NAME = "vt100-terminal.in";
    public final static String DEFAULT_OUTPUT_FILE_NAME = "vt100-terminal.out";
    private final static String INPUT_FILE_NAME = "inputFileName";
    private final static String OUTPUT_FILE_NAME = "outputFileName";
    private final static String INPUT_READ_DELAY_MILLIS = "inputReadDelayMillis";

    private final Dialogs dialogs;
    private final PluginSettings settings;
    private final boolean guiSupported;
    private final List<ChangedObserver> observers = new ArrayList<>();
    private volatile Path inputPath = Path.of(DEFAULT_INPUT_FILE_NAME);
    private volatile Path outputPath = Path.of(DEFAULT_OUTPUT_FILE_NAME);
    private int inputReadDelayMillis = 0;

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

    public int getInputReadDelayMillis() {
        return inputReadDelayMillis;
    }

    public void setInputReadDelayMillis(int inputReadDelayMillis) {
        this.inputReadDelayMillis = inputReadDelayMillis;
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

    public void write() {
        try {
            settings.setInt(INPUT_READ_DELAY_MILLIS, inputReadDelayMillis);
            settings.setString(OUTPUT_FILE_NAME, outputPath.toString());
            settings.setString(INPUT_FILE_NAME, inputPath.toString());
        } catch (CannotUpdateSettingException e) {
            LOGGER.error("Could not update settings", e);
            dialogs.showError("Could not save settings. Please see log file for details.", "VT100 Terminal");
        } finally {
            notifyObserversAndIgnoreError();
        }
    }

    private void readSettings() {
        inputPath = Path.of(settings.getString(INPUT_FILE_NAME, DEFAULT_INPUT_FILE_NAME));
        outputPath = Path.of(settings.getString(OUTPUT_FILE_NAME, DEFAULT_OUTPUT_FILE_NAME));
        try {
            inputReadDelayMillis = settings.getInt(INPUT_READ_DELAY_MILLIS, 0);
        } catch (NumberFormatException e) {
            inputReadDelayMillis = 0;
            LOGGER.error(
                    "Could not read '" + INPUT_READ_DELAY_MILLIS + "' setting. Using default value ({})", inputReadDelayMillis, e
            );
        }

        if (inputPath.toString().equals(outputPath.toString())) {
            LOGGER.error("VT100 Terminal settings: Input path is not allowed to be equal to the output path. Setting to default.");
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

    public interface ChangedObserver {
        void settingsChanged() throws IOException;
    }
}
