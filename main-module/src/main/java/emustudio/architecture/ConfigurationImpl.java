package emustudio.architecture;

import emulib.emustudio.SettingsManager;
import emustudio.drawing.Schema;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public class ConfigurationImpl implements Configuration {
    private final String configurationName;
    private final Properties properties;

    public ConfigurationImpl(String configurationName, Properties properties) {
        this.configurationName = Objects.requireNonNull(configurationName);
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public String get(String key) {
        return properties.getProperty(key);
    }

    @Override
    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    @Override
    public void set(String key, String value) {
        properties.setProperty(key, value);
    }

    @Override
    public void remove(String key) {
        properties.remove(key);
    }

    @Override
    public boolean contains(String key) {
        return properties.containsKey(key);
    }

    @Override
    public void write() throws WriteConfigurationException {
        if ((configurationName == null) || configurationName.isEmpty()) {
            throw new WriteConfigurationException("Configuration name is not set");
        }
        Path dirPath = ComputerConfig.getConfigDir();
        File dirFile = dirPath.toFile();
        if (!dirFile.exists() || (dirFile.exists() && !dirFile.isDirectory())) {
            if (!dirFile.mkdir()) {
                throw new WriteConfigurationException("Could not create config directory");
            }
        }
        try {
            File configFile = dirPath.resolve(configurationName + ".conf").toFile();
            configFile.createNewFile();
            try (FileOutputStream out = new FileOutputStream(configFile)) {
                properties.put("emu8Version", "4");
                String noGUI = (String) properties.remove(SettingsManager.NO_GUI);
                String auto = (String) properties.remove(SettingsManager.AUTO);

                properties.store(out, configurationName + " configuration file");

                if (noGUI != null) {
                    properties.put(SettingsManager.NO_GUI, noGUI);
                }
                if (auto != null) {
                    properties.put(SettingsManager.AUTO, auto);
                }
            }
        } catch (IOException e) {
            throw new WriteConfigurationException("Could not save configuration.", e);
        }
    }

    @Override
    public Schema loadSchema() {
        return new Schema(configurationName, properties);
    }

}
