package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.emulib.runtime.settings.BasicSettings;
import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BasicSettingsImpl implements BasicSettings {
    private final Config config;
    private final Runnable save;

    public BasicSettingsImpl(Config config, Runnable save) {
        this.config = Objects.requireNonNull(config);
        this.save = Objects.requireNonNull(save);
    }

    @Override
    public boolean contains(String key) {
        return config.contains(key);
    }

    @Override
    public void remove(String key) throws CannotUpdateSettingException {
        config.remove(key);
        save.run();
    }

    @Override
    public Optional<String> getString(String key) {
        return config.getOptional(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return config.<String>getOptional(key).orElse(defaultValue);
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        return config.getOptional(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return config.<Boolean>getOptional(key).orElse(defaultValue);
    }

    @Override
    public Optional<Integer> getInt(String key) {
        return config.getOptional(key);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return config.getOptionalInt(key).orElse(defaultValue);
    }

    @Override
    public Optional<Long> getLong(String key) {
        return config.getOptional(key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return config.getOptionalLong(key).orElse(defaultValue);
    }

    @Override
    public Optional<Double> getDouble(String key) {
        return config.getOptional(key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return config.<Double>getOptional(key).orElse(defaultValue);
    }

    @Override
    public List<String> getArray(String key) {
        return config.<List<String>>getOptional(key).orElse(Collections.emptyList());
    }

    @Override
    public List<String> getArray(String key, List<String> defaultValue) {
        return config.<List<String>>getOptional(key).orElse(defaultValue);
    }

    @Override
    public void setString(String key, String value) {
        config.set(key, value);
        save.run();
    }

    @Override
    public void setBoolean(String key, boolean value) throws CannotUpdateSettingException {
        config.set(key, value);
        save.run();
    }

    @Override
    public void setInt(String key, int value) throws CannotUpdateSettingException {
        config.set(key, value);
        save.run();
    }

    @Override
    public void setLong(String key, long value) throws CannotUpdateSettingException {
        config.set(key, value);
        save.run();
    }

    @Override
    public void setDouble(String key, double value) throws CannotUpdateSettingException {
        config.set(key, value);
        save.run();
    }

    @Override
    public void setArray(String key, List<String> value) throws CannotUpdateSettingException {
        config.set(key, value);
        save.run();
    }

    @Override
    public Optional<BasicSettings> getSubSettings(String key) {
        return config.<Config>getOptional(key).map(c -> new BasicSettingsImpl(c, save));
    }

    @Override
    public BasicSettings setSubSettings(String key) throws CannotUpdateSettingException {
        Config newConfig = config.createSubConfig();
        config.set(key, newConfig);
        save.run();
        return new BasicSettingsImpl(newConfig, save);
    }
}
