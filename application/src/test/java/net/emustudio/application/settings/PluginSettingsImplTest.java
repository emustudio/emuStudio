/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class PluginSettingsImplTest {

    @Test
    public void prefixedKeysDelegateToApplicationSettings() {
        AppSettings application = new AppSettings(Config.inMemory(), false, false);
        application.setString("sharedString", "value");
        application.setBoolean("sharedBoolean", true);
        application.setInt("sharedInt", 7);
        application.setLong("sharedLong", 8L);
        application.setDouble("sharedDouble", 1.25);
        application.setArray("sharedArray", Arrays.asList("a", "b"));

        PluginSettingsImpl settings = new PluginSettingsImpl(Config.inMemory(), application, new Runnable() {
            @Override
            public void run() {
            }
        });

        assertTrue(settings.contains(PluginSettings.EMUSTUDIO_PREFIX + "sharedString"));
        assertEquals(Optional.of("value"), settings.getString(PluginSettings.EMUSTUDIO_PREFIX + "sharedString"));
        assertTrue(settings.getBoolean(PluginSettings.EMUSTUDIO_PREFIX + "sharedBoolean", false));
        assertEquals(7, settings.getInt(PluginSettings.EMUSTUDIO_PREFIX + "sharedInt", -1));
        assertEquals(8L, settings.getLong(PluginSettings.EMUSTUDIO_PREFIX + "sharedLong", -1L));
        assertEquals(1.25, settings.getDouble(PluginSettings.EMUSTUDIO_PREFIX + "sharedDouble", -1), 0.0);
        assertEquals(Arrays.asList("a", "b"), settings.getArray(PluginSettings.EMUSTUDIO_PREFIX + "sharedArray"));
    }

    @Test
    public void localKeysAreStoredLocallyAndTriggerSaveCallback() {
        AtomicInteger saveCalls = new AtomicInteger();
        PluginSettingsImpl settings = new PluginSettingsImpl(
                Config.inMemory(),
                new AppSettings(Config.inMemory(), false, false),
                new Runnable() {
                    @Override
                    public void run() {
                        saveCalls.incrementAndGet();
                    }
                }
        );

        settings.setString("local", "value");
        settings.setSubSettings("nested").setInt("count", 4);

        assertEquals(Optional.of("value"), settings.getString("local"));
        assertEquals(3, saveCalls.get());
        assertTrue(settings.getSubSettings("nested").isPresent());
        assertEquals(Optional.of(4), settings.getSubSettings("nested").get().getInt("count"));
    }

    @Test
    public void mutatingPrefixedKeysIsRejected() {
        final PluginSettingsImpl settings = new PluginSettingsImpl(
                Config.inMemory(),
                new AppSettings(Config.inMemory(), false, false),
                new Runnable() {
                    @Override
                    public void run() {
                    }
                }
        );

        expectIllegalArgument(new Runnable() {
            @Override
            public void run() {
                settings.setString(PluginSettings.EMUSTUDIO_PREFIX + "key", "value");
            }
        });
        expectIllegalArgument(new Runnable() {
            @Override
            public void run() {
                settings.remove(PluginSettings.EMUSTUDIO_PREFIX + "key");
            }
        });
        expectIllegalArgument(new Runnable() {
            @Override
            public void run() {
                settings.getSubSettings(PluginSettings.EMUSTUDIO_PREFIX + "key");
            }
        });
        expectIllegalArgument(new Runnable() {
            @Override
            public void run() {
                settings.setSubSettings(PluginSettings.EMUSTUDIO_PREFIX + "key");
            }
        });
    }

    private void expectIllegalArgument(Runnable runnable) {
        try {
            runnable.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(PluginSettings.EMUSTUDIO_PREFIX));
        }
    }
}
