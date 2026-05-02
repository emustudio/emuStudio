/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem;

import net.emustudio.emulib.runtime.settings.PluginSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.replay;

public final class PluginSettingsMock {
    private final Map<String, Object> values = new HashMap<>();
    private final PluginSettings mock = createMock(PluginSettings.class);

    public PluginSettingsMock() {
        expect(mock.contains(anyString())).andAnswer(() -> values.containsKey(key())).anyTimes();
        mock.remove(anyString());
        expectLastCall().andAnswer(() -> {
            values.remove(key());
            return null;
        }).anyTimes();
        expect(mock.getString(anyString())).andAnswer(() -> Optional.ofNullable((String) values.get(key()))).anyTimes();
        expect(mock.getInt(anyString())).andAnswer(() -> Optional.ofNullable((Integer) values.get(key()))).anyTimes();
        expect(mock.getBoolean(anyString(), anyBoolean())).andAnswer(() -> {
            Boolean value = (Boolean) values.get(key());
            return value != null ? value : (Boolean) getCurrentArguments()[1];
        }).anyTimes();
        expect(mock.getInt(anyString(), anyInt())).andAnswer(() -> {
            Integer value = (Integer) values.get(key());
            return value != null ? value : (Integer) getCurrentArguments()[1];
        }).anyTimes();
        mock.setString(anyString(), anyString());
        expectLastCall().andAnswer(() -> {
            Object[] args = getCurrentArguments();
            values.put((String) args[0], args[1]);
            return null;
        }).anyTimes();
        mock.setInt(anyString(), anyInt());
        expectLastCall().andAnswer(() -> {
            Object[] args = getCurrentArguments();
            values.put((String) args[0], args[1]);
            return null;
        }).anyTimes();
        replay(mock);
    }

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
        return Optional.ofNullable((String) values.get(key));
    }

    public Optional<Integer> getInt(String key) {
        return Optional.ofNullable((Integer) values.get(key));
    }

    public int getInt(String key, int defaultValue) {
        return getInt(key).orElse(defaultValue);
    }

    private static String key() {
        return (String) getCurrentArguments()[0];
    }
}
