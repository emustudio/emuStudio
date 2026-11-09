/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.emulib.application.settings.PluginSettings;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class PluginSettingsImplTest {
    private static final String PREFIX = PluginSettings.EMUSTUDIO_PREFIX;
    private static final Runnable NOOP = () -> {};

    private AppSettings application;
    private Config pluginConfig;

    @Before
    public void setUp() {
        application = new AppSettings(Config.inMemory(), false, false);
        pluginConfig = Config.inMemory();
    }

    private PluginSettingsImpl createSettings() {
        return new PluginSettingsImpl(pluginConfig, application, NOOP);
    }

    private PluginSettingsImpl createSettings(Runnable save) {
        return new PluginSettingsImpl(pluginConfig, application, save);
    }

    @Test(expected = NullPointerException.class)
    public void constructorRejectsNullApplication() {
        new PluginSettingsImpl(Config.inMemory(), null, NOOP);
    }

    @Test
    public void prefixedKeysDelegateToApplicationSettings() {
        application.set("sharedString", "value");
        application.set("sharedBoolean", true);
        application.set("sharedInt", 7);
        application.set("sharedLong", 8L);
        application.set("sharedDouble", 1.25);
        application.set("sharedArray", Arrays.asList("a", "b"));

        PluginSettingsImpl settings = createSettings();

        assertEquals(Optional.of("value"), settings.getString(PREFIX + "sharedString"));
        assertEquals(Optional.of(true), settings.getBoolean(PREFIX + "sharedBoolean"));
        assertEquals(Optional.of(7), settings.getInt(PREFIX + "sharedInt"));
        assertEquals(Optional.of(8L), settings.getLong(PREFIX + "sharedLong"));
        assertEquals(Optional.of(1.25), settings.getDouble(PREFIX + "sharedDouble"));
        assertEquals(Arrays.asList("a", "b"), settings.getArray(PREFIX + "sharedArray"));
    }

    @Test
    public void prefixedNoGuiAndAutoAreReadFromApplication() {
        application = new AppSettings(Config.inMemory(), true, false);
        PluginSettingsImpl settings = createSettings();

        assertEquals(Optional.of(true), settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI));
        assertEquals(Optional.of(false), settings.getBoolean(PluginSettings.EMUSTUDIO_AUTO));
    }

    @Test
    public void prefixedKeysReturnEmptyWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertFalse(settings.getString(PREFIX + "missing").isPresent());
        assertFalse(settings.getBoolean(PREFIX + "missing").isPresent());
        assertFalse(settings.getInt(PREFIX + "missing").isPresent());
        assertFalse(settings.getLong(PREFIX + "missing").isPresent());
        assertFalse(settings.getDouble(PREFIX + "missing").isPresent());
        assertEquals(Collections.emptyList(), settings.getArray(PREFIX + "missing"));
    }

    @Test
    public void localKeysAreStoredLocallyAndTriggerSaveCallback() {
        AtomicInteger saveCalls = new AtomicInteger();
        PluginSettingsImpl settings = createSettings(saveCalls::incrementAndGet);

        settings.set("local", "value");
        settings.setSubSettings("nested").set("count", 4);

        assertEquals(Optional.of("value"), settings.getString("local"));
        assertEquals(3, saveCalls.get());
        assertTrue(settings.getSubSettings("nested").isPresent());
        assertEquals(Optional.of(4), settings.getSubSettings("nested").get().getInt("count"));
    }

    @Test
    public void setAndGetAllSupportedTypes() {
        AtomicInteger saveCalls = new AtomicInteger();
        PluginSettingsImpl settings = createSettings(saveCalls::incrementAndGet);
        List<String> items = Arrays.asList("one", "two", "three");

        settings.set("name", "emuStudio");
        settings.set("flag", true);
        settings.set("count", 42);
        settings.set("bigNum", 100000L);
        settings.set("ratio", 2.718);
        settings.set("list", items);

        assertEquals(Optional.of("emuStudio"), settings.getString("name"));
        assertEquals(Optional.of(true), settings.getBoolean("flag"));
        assertEquals(Optional.of(42), settings.getInt("count"));
        assertEquals(Optional.of(100000L), settings.getLong("bigNum"));
        assertEquals(Optional.of(2.718), settings.getDouble("ratio"));
        assertEquals(items, settings.getArray("list"));
        assertEquals(6, saveCalls.get());
    }

    @Test
    public void missingKeysReturnEmpty() {
        PluginSettingsImpl settings = createSettings();

        assertFalse(settings.getString("missing").isPresent());
        assertFalse(settings.getBoolean("missing").isPresent());
        assertFalse(settings.getInt("missing").isPresent());
        assertFalse(settings.getLong("missing").isPresent());
        assertFalse(settings.getDouble("missing").isPresent());
        assertEquals(Collections.emptyList(), settings.getArray("missing"));
        assertFalse(settings.getSubSettings("missing").isPresent());
    }

    @Test
    public void valuesOfDifferentTypeReturnEmpty() {
        PluginSettingsImpl settings = createSettings();
        settings.set("text", "value");
        settings.set("tooBig", Long.MAX_VALUE);

        assertFalse(settings.getBoolean("text").isPresent());
        assertFalse(settings.getInt("text").isPresent());
        assertFalse(settings.getDouble("text").isPresent());
        assertEquals(Collections.emptyList(), settings.getArray("text"));
        assertFalse(settings.getSubSettings("text").isPresent());
        assertFalse(settings.getInt("tooBig").isPresent());
    }

    @Test
    public void integralNumbersAreConvertedBetweenNumericTypes() {
        PluginSettingsImpl settings = createSettings();
        pluginConfig.set("int", 5);
        pluginConfig.set("long", 6L);

        assertEquals(Optional.of(5L), settings.getLong("int"));
        assertEquals(Optional.of(6), settings.getInt("long"));
        assertEquals(Optional.of(5.0), settings.getDouble("int"));
    }

    @Test
    public void setNullRemovesKeyAndTriggersSave() {
        AtomicInteger saveCalls = new AtomicInteger();
        PluginSettingsImpl settings = createSettings(saveCalls::incrementAndGet);

        settings.set("toRemove", "value");
        assertTrue(settings.getString("toRemove").isPresent());
        saveCalls.set(0);

        settings.set("toRemove", null);

        assertFalse(settings.getString("toRemove").isPresent());
        assertEquals(1, saveCalls.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setRejectsUnsupportedValueType() {
        createSettings().set("key", new Object());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setRejectsListOfNonStrings() {
        createSettings().set("key", List.of(1, 2));
    }

    @Test
    public void mutatingPrefixedKeysIsRejected() {
        final PluginSettingsImpl settings = createSettings();

        expectIllegalArgument(() -> settings.set(PREFIX + "key", "value"));
        expectIllegalArgument(() -> settings.set(PREFIX + "key", null));
        expectIllegalArgument(() -> settings.getSubSettings(PREFIX + "key"));
        expectIllegalArgument(() -> settings.setSubSettings(PREFIX + "key"));
    }

    @Test
    public void subSettingsShareSaveCallbackAndApplication() {
        application.set("shared", "app");
        AtomicInteger saveCalls = new AtomicInteger();
        PluginSettingsImpl settings = createSettings(saveCalls::incrementAndGet);

        PluginSettings subSettings = settings.setSubSettings("nested");
        subSettings.set("message", "hello");

        assertEquals(Optional.of("hello"), settings.getSubSettings("nested").orElseThrow().getString("message"));
        assertEquals(Optional.of("app"), subSettings.getString(PREFIX + "shared"));
        assertEquals(2, saveCalls.get());
    }

    private void expectIllegalArgument(Runnable runnable) {
        try {
            runnable.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains(PREFIX));
        }
    }
}
