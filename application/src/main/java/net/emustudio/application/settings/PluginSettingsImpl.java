/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.emulib.runtime.settings.PluginSettings;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Plugin settings backed by a configuration subtree.
 * <p>
 * Keys starting with {@link #EMUSTUDIO_PREFIX} are read-only and delegated to the application settings.
 */
public class PluginSettingsImpl implements PluginSettings {
    private final Config config;
    private final PluginSettings application;
    private final Runnable save;

    public PluginSettingsImpl(Config config, PluginSettings application, Runnable save) {
        this.config = Objects.requireNonNull(config);
        this.application = Objects.requireNonNull(application);
        this.save = Objects.requireNonNull(save);
    }

    @Override
    public Optional<String> getString(String key) {
        if (isApplicationKey(key)) {
            return application.getString(applicationKey(key));
        }
        return get(key, String.class);
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        if (isApplicationKey(key)) {
            return application.getBoolean(applicationKey(key));
        }
        return get(key, Boolean.class);
    }

    @Override
    public Optional<Integer> getInt(String key) {
        if (isApplicationKey(key)) {
            return application.getInt(applicationKey(key));
        }
        return getIntegral(key)
                .filter(value -> value.longValue() == value.intValue())
                .map(Number::intValue);
    }

    @Override
    public Optional<Long> getLong(String key) {
        if (isApplicationKey(key)) {
            return application.getLong(applicationKey(key));
        }
        return getIntegral(key).map(Number::longValue);
    }

    @Override
    public Optional<Double> getDouble(String key) {
        if (isApplicationKey(key)) {
            return application.getDouble(applicationKey(key));
        }
        return get(key, Number.class).map(Number::doubleValue);
    }

    @Override
    public List<String> getArray(String key) {
        if (isApplicationKey(key)) {
            return application.getArray(applicationKey(key));
        }
        return get(key, List.class)
                .<List<?>>map(list -> list)
                .filter(PluginSettingsImpl::isStringList)
                .map(list -> list.stream().map(String.class::cast).collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Override
    public Optional<PluginSettings> getSubSettings(String key) {
        checkNotApplicationKey(key);
        return get(key, Config.class).map(c -> new PluginSettingsImpl(c, application, save));
    }

    @Override
    public PluginSettings setSubSettings(String key) {
        checkNotApplicationKey(key);
        Config subConfig = config.createSubConfig();
        config.set(key, subConfig);
        save.run();
        return new PluginSettingsImpl(subConfig, application, save);
    }

    @Override
    public void set(String key, Object value) {
        checkNotApplicationKey(key);
        if (value == null) {
            config.remove(key);
        } else if (isSupported(value)) {
            config.set(key, value);
        } else {
            throw new IllegalArgumentException("Unsupported setting value type: " + value.getClass());
        }
        save.run();
    }

    private <T> Optional<T> get(String key, Class<T> type) {
        Object value = config.get(key);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    private Optional<Number> getIntegral(String key) {
        return get(key, Number.class).filter(value -> value instanceof Integer || value instanceof Long);
    }

    private static boolean isSupported(Object value) {
        if (value instanceof List) {
            return isStringList((List<?>) value);
        }
        return value instanceof String || value instanceof Boolean || value instanceof Integer
                || value instanceof Long || value instanceof Double;
    }

    private static boolean isStringList(List<?> list) {
        return list.stream().allMatch(item -> item instanceof String);
    }

    private static boolean isApplicationKey(String key) {
        return key.startsWith(EMUSTUDIO_PREFIX);
    }

    private static String applicationKey(String key) {
        return key.substring(EMUSTUDIO_PREFIX.length());
    }

    private static void checkNotApplicationKey(String key) {
        if (isApplicationKey(key)) {
            throw new IllegalArgumentException("Key cannot start with " + EMUSTUDIO_PREFIX);
        }
    }
}
