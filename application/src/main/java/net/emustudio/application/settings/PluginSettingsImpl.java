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
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.emulib.runtime.settings.BasicSettings;
import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.settings.PluginSettings;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PluginSettingsImpl extends BasicSettingsImpl implements PluginSettings {
    private final AppSettings application;

    public PluginSettingsImpl(Config pluginConfig, AppSettings application, Runnable save) {
        super(pluginConfig, save);
        this.application = Objects.requireNonNull(application);
    }

    @Override
    public boolean contains(String key) {
        if (key.startsWith(EMUSTUDIO_PREFIX)) {
            return application.contains(key.substring(EMUSTUDIO_PREFIX.length()));
        }
        return super.contains(key);
    }

    @Override
    public void remove(String key) {
        checkDoesNotStartWithEmuStudioPrefix(key);
        super.remove(key);
    }

    @Override
    public Optional<String> getString(String key) {
        if (key.startsWith(EMUSTUDIO_PREFIX)) {
            return application.getString(key.substring(EMUSTUDIO_PREFIX.length()));
        }
        return super.getString(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        if (key.startsWith(EMUSTUDIO_PREFIX)) {
            return application.getString(key.substring(EMUSTUDIO_PREFIX.length())).orElse(defaultValue);
        }
        return super.getString(key, defaultValue);
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        if (key.startsWith(EMUSTUDIO_PREFIX)) {
            return application.getBoolean(key.substring(EMUSTUDIO_PREFIX.length()));
        }
        return super.getBoolean(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        if (key.startsWith(EMUSTUDIO_PREFIX)) {
            return application.getBoolean(key.substring(EMUSTUDIO_PREFIX.length())).orElse(defaultValue);
        }
        return super.getBoolean(key, defaultValue);
    }

    @Override
    public Optional<Integer> getInt(String key) {
        if (key.startsWith(EMUSTUDIO_PREFIX)) {
            return application.getInt(key.substring(EMUSTUDIO_PREFIX.length()));
        }
        return super.getInt(key);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        if (key.startsWith(EMUSTUDIO_PREFIX)) {
            return application.getInt(key.substring(EMUSTUDIO_PREFIX.length())).orElse(defaultValue);
        }
        return super.getInt(key, defaultValue);
    }

    @Override
    public Optional<Long> getLong(String key) {
        if (key.startsWith(EMUSTUDIO_PREFIX)) {
            return application.getLong(key.substring(EMUSTUDIO_PREFIX.length()));
        }
        return super.getLong(key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        if (key.startsWith(EMUSTUDIO_PREFIX)) {
            return application.getLong(key.substring(EMUSTUDIO_PREFIX.length())).orElse(defaultValue);
        }
        return super.getLong(key, defaultValue);
    }

    @Override
    public Optional<Double> getDouble(String key) {
        if (key.startsWith(EMUSTUDIO_PREFIX)) {
            return application.getDouble(key.substring(EMUSTUDIO_PREFIX.length()));
        }
        return super.getDouble(key);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        if (key.startsWith(EMUSTUDIO_PREFIX)) {
            return application.getDouble(key.substring(EMUSTUDIO_PREFIX.length())).orElse(defaultValue);
        }
        return super.getDouble(key, defaultValue);
    }

    @Override
    public List<String> getArray(String key) {
        if (key.startsWith(EMUSTUDIO_PREFIX)) {
            return application.getArray(key.substring(EMUSTUDIO_PREFIX.length()));
        }
        return super.getArray(key, Collections.emptyList());
    }

    @Override
    public List<String> getArray(String key, List<String> defaultValue) {
        if (key.startsWith(EMUSTUDIO_PREFIX)) {
            return application.getArray(key.substring(EMUSTUDIO_PREFIX.length()));
        }
        return super.getArray(key, defaultValue);
    }

    @Override
    public void setString(String key, String value) {
        checkDoesNotStartWithEmuStudioPrefix(key);
        super.setString(key, value);
    }

    @Override
    public void setBoolean(String key, boolean value) {
        checkDoesNotStartWithEmuStudioPrefix(key);
        super.setBoolean(key, value);
    }

    @Override
    public void setInt(String key, int value) {
        checkDoesNotStartWithEmuStudioPrefix(key);
        super.setInt(key, value);
    }

    @Override
    public void setLong(String key, long value) {
        checkDoesNotStartWithEmuStudioPrefix(key);
        super.setLong(key, value);
    }

    @Override
    public void setDouble(String key, double value) {
        checkDoesNotStartWithEmuStudioPrefix(key);
        super.setDouble(key, value);
    }

    @Override
    public void setArray(String key, List<String> value) {
        checkDoesNotStartWithEmuStudioPrefix(key);
        super.setArray(key, value);
    }

    @Override
    public Optional<BasicSettings> getSubSettings(String key) {
        checkDoesNotStartWithEmuStudioPrefix(key);
        return super.getSubSettings(key);
    }

    @Override
    public BasicSettings setSubSettings(String key) throws CannotUpdateSettingException {
        checkDoesNotStartWithEmuStudioPrefix(key);
        return super.setSubSettings(key);
    }

    private void checkDoesNotStartWithEmuStudioPrefix(String key) {
        if (key.startsWith(EMUSTUDIO_PREFIX)) {
            throw new IllegalArgumentException("Key cannot start with " + EMUSTUDIO_PREFIX);
        }
    }
}
