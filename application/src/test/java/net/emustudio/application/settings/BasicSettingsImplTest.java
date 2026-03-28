/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.emulib.runtime.settings.BasicSettings;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class BasicSettingsImplTest {

    @Test
    public void settersAndRemoveUpdateConfigAndInvokeSaveCallback() {
        Config config = Config.inMemory();
        AtomicInteger saveCalls = new AtomicInteger();
        BasicSettingsImpl settings = new BasicSettingsImpl(config, new Runnable() {
            @Override
            public void run() {
                saveCalls.incrementAndGet();
            }
        });

        settings.setString("name", "emuStudio");
        settings.setBoolean("enabled", true);
        settings.setInt("count", 3);
        settings.setLong("size", 4L);
        settings.setDouble("ratio", 2.5);
        settings.setArray("items", Arrays.asList("a", "b"));
        settings.remove("enabled");

        assertEquals(Optional.of("emuStudio"), settings.getString("name"));
        assertFalse(settings.getBoolean("enabled").isPresent());
        assertEquals(Optional.of(3), settings.getInt("count"));
        assertEquals(Optional.of(4L), settings.getLong("size"));
        assertEquals(Optional.of(2.5), settings.getDouble("ratio"));
        assertEquals(Arrays.asList("a", "b"), settings.getArray("items"));
        assertEquals(7, saveCalls.get());
    }

    @Test
    public void subSettingsShareSaveCallbackAndDefaults() {
        Config config = Config.inMemory();
        AtomicInteger saveCalls = new AtomicInteger();
        BasicSettingsImpl settings = new BasicSettingsImpl(config, new Runnable() {
            @Override
            public void run() {
                saveCalls.incrementAndGet();
            }
        });

        BasicSettings subSettings = settings.setSubSettings("nested");
        subSettings.setString("message", "hello");

        assertEquals("fallback", settings.getString("missing", "fallback"));
        assertEquals(9, settings.getInt("missingInt", 9));
        assertEquals(Arrays.asList("x"), settings.getArray("missingArray", Arrays.asList("x")));
        assertTrue(settings.getSubSettings("nested").isPresent());
        assertEquals(Optional.of("hello"), settings.getSubSettings("nested").get().getString("message"));
        assertEquals(2, saveCalls.get());
    }
}
