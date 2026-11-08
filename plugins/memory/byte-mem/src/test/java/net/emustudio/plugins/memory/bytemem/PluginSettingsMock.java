/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem;

import net.emustudio.emulib.runtime.settings.PluginSettings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PluginSettingsMock {
    private final Map<String, Object> values = new HashMap<>();
    private final PluginSettings mock = new PluginSettings() {
        @Override
        public Optional<String> getString(String key) {
            return typed(key, String.class);
        }

        @Override
        public Optional<Boolean> getBoolean(String key) {
            return typed(key, Boolean.class);
        }

        @Override
        public Optional<Integer> getInt(String key) {
            return typed(key, Integer.class);
        }

        @Override
        public Optional<Long> getLong(String key) {
            return typed(key, Long.class);
        }

        @Override
        public Optional<Double> getDouble(String key) {
            return typed(key, Double.class);
        }

        @Override
        public List<String> getArray(String key) {
            return List.of();
        }

        @Override
        public Optional<PluginSettings> getSubSettings(String key) {
            return Optional.empty();
        }

        @Override
        public PluginSettings setSubSettings(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(String key, Object value) {
            if (value == null) {
                values.remove(key);
            } else {
                values.put(key, value);
            }
        }
    };

    public PluginSettingsMock set(String key, Object value) {
        values.put(key, value);
        return this;
    }

    public PluginSettings mock() {
        return mock;
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }

    public Optional<String> getString(String key) {
        return typed(key, String.class);
    }

    public Optional<Integer> getInt(String key) {
        return typed(key, Integer.class);
    }

    public int getInt(String key, int defaultValue) {
        return getInt(key).orElse(defaultValue);
    }

    private <T> Optional<T> typed(String key, Class<T> type) {
        Object value = values.get(key);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }
}
