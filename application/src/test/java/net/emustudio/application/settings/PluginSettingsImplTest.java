/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.emulib.runtime.settings.PluginSettings;
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
        application.setString("sharedString", "value");
        application.setBoolean("sharedBoolean", true);
        application.setInt("sharedInt", 7);
        application.setLong("sharedLong", 8L);
        application.setDouble("sharedDouble", 1.25);
        application.setArray("sharedArray", Arrays.asList("a", "b"));

        PluginSettingsImpl settings = createSettings();

        assertTrue(settings.contains(PREFIX + "sharedString"));
        assertEquals(Optional.of("value"), settings.getString(PREFIX + "sharedString"));
        assertTrue(settings.getBoolean(PREFIX + "sharedBoolean", false));
        assertEquals(7, settings.getInt(PREFIX + "sharedInt", -1));
        assertEquals(8L, settings.getLong(PREFIX + "sharedLong", -1L));
        assertEquals(1.25, settings.getDouble(PREFIX + "sharedDouble", -1), 0.0);
        assertEquals(Arrays.asList("a", "b"), settings.getArray(PREFIX + "sharedArray"));
    }

    @Test
    public void prefixedGetStringWithDefaultDelegatesToApplication() {
        application.setString("appKey", "appValue");
        PluginSettingsImpl settings = createSettings();

        assertEquals("appValue", settings.getString(PREFIX + "appKey", "fallback"));
    }

    @Test
    public void prefixedGetStringWithDefaultReturnsFallbackWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertEquals("fallback", settings.getString(PREFIX + "missingKey", "fallback"));
    }

    @Test
    public void prefixedGetBooleanOptionalDelegatesToApplication() {
        application.setBoolean("flag", true);
        PluginSettingsImpl settings = createSettings();

        assertEquals(Optional.of(true), settings.getBoolean(PREFIX + "flag"));
    }

    @Test
    public void prefixedGetBooleanOptionalReturnsEmptyWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertFalse(settings.getBoolean(PREFIX + "missing").isPresent());
    }

    @Test
    public void prefixedGetIntOptionalDelegatesToApplication() {
        application.setInt("count", 42);
        PluginSettingsImpl settings = createSettings();

        assertEquals(Optional.of(42), settings.getInt(PREFIX + "count"));
    }

    @Test
    public void prefixedGetIntOptionalReturnsEmptyWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertFalse(settings.getInt(PREFIX + "missing").isPresent());
    }

    @Test
    public void prefixedGetLongOptionalDelegatesToApplication() {
        application.setLong("bigNum", 999L);
        PluginSettingsImpl settings = createSettings();

        assertEquals(Optional.of(999L), settings.getLong(PREFIX + "bigNum"));
    }

    @Test
    public void prefixedGetLongOptionalReturnsEmptyWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertFalse(settings.getLong(PREFIX + "missing").isPresent());
    }

    @Test
    public void prefixedGetDoubleOptionalDelegatesToApplication() {
        application.setDouble("ratio", 3.14);
        PluginSettingsImpl settings = createSettings();

        assertEquals(Optional.of(3.14), settings.getDouble(PREFIX + "ratio"));
    }

    @Test
    public void prefixedGetDoubleOptionalReturnsEmptyWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertFalse(settings.getDouble(PREFIX + "missing").isPresent());
    }

    @Test
    public void prefixedGetArrayWithDefaultDelegatesToApplication() {
        application.setArray("items", Arrays.asList("x", "y"));
        PluginSettingsImpl settings = createSettings();

        assertEquals(Arrays.asList("x", "y"), settings.getArray(PREFIX + "items", Collections.singletonList("z")));
    }

    @Test
    public void prefixedGetBooleanDefaultReturnsFallbackWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertTrue(settings.getBoolean(PREFIX + "missing", true));
    }

    @Test
    public void prefixedGetIntDefaultReturnsFallbackWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertEquals(99, settings.getInt(PREFIX + "missing", 99));
    }

    @Test
    public void prefixedGetLongDefaultReturnsFallbackWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertEquals(123L, settings.getLong(PREFIX + "missing", 123L));
    }

    @Test
    public void prefixedGetDoubleDefaultReturnsFallbackWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertEquals(2.71, settings.getDouble(PREFIX + "missing", 2.71), 0.0);
    }

    @Test
    public void prefixedContainsReturnsFalseWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertFalse(settings.contains(PREFIX + "nonexistent"));
    }

    @Test
    public void localKeysAreStoredLocallyAndTriggerSaveCallback() {
        AtomicInteger saveCalls = new AtomicInteger();
        PluginSettingsImpl settings = createSettings(saveCalls::incrementAndGet);

        settings.setString("local", "value");
        settings.setSubSettings("nested").setInt("count", 4);

        assertEquals(Optional.of("value"), settings.getString("local"));
        assertEquals(3, saveCalls.get());
        assertTrue(settings.getSubSettings("nested").isPresent());
        assertEquals(Optional.of(4), settings.getSubSettings("nested").get().getInt("count"));
    }

    @Test
    public void localContainsReturnsTrueForExistingKey() {
        PluginSettingsImpl settings = createSettings();
        settings.setString("exists", "yes");

        assertTrue(settings.contains("exists"));
    }

    @Test
    public void localContainsReturnsFalseForMissingKey() {
        PluginSettingsImpl settings = createSettings();

        assertFalse(settings.contains("nope"));
    }

    @Test
    public void localRemoveDeletesKeyAndTriggersSave() {
        AtomicInteger saveCalls = new AtomicInteger();
        PluginSettingsImpl settings = createSettings(saveCalls::incrementAndGet);

        settings.setString("toRemove", "value");
        assertTrue(settings.contains("toRemove"));
        saveCalls.set(0);

        settings.remove("toRemove");

        assertFalse(settings.contains("toRemove"));
        assertEquals(1, saveCalls.get());
    }

    @Test
    public void localGetStringWithDefault() {
        PluginSettingsImpl settings = createSettings();
        settings.setString("key", "actual");

        assertEquals("actual", settings.getString("key", "fallback"));
        assertEquals("fallback", settings.getString("missing", "fallback"));
    }

    @Test
    public void localGetStringOptionalReturnsEmptyWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertFalse(settings.getString("missing").isPresent());
    }

    @Test
    public void localSetAndGetBoolean() {
        AtomicInteger saveCalls = new AtomicInteger();
        PluginSettingsImpl settings = createSettings(saveCalls::incrementAndGet);

        settings.setBoolean("flag", true);

        assertEquals(Optional.of(true), settings.getBoolean("flag"));
        assertTrue(settings.getBoolean("flag", false));
        assertEquals(1, saveCalls.get());
    }

    @Test
    public void localGetBooleanDefaultWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertFalse(settings.getBoolean("missing").isPresent());
        assertTrue(settings.getBoolean("missing", true));
        assertFalse(settings.getBoolean("missing", false));
    }

    @Test
    public void localSetAndGetInt() {
        AtomicInteger saveCalls = new AtomicInteger();
        PluginSettingsImpl settings = createSettings(saveCalls::incrementAndGet);

        settings.setInt("count", 42);

        assertEquals(Optional.of(42), settings.getInt("count"));
        assertEquals(42, settings.getInt("count", -1));
        assertEquals(1, saveCalls.get());
    }

    @Test
    public void localGetIntDefaultWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertFalse(settings.getInt("missing").isPresent());
        assertEquals(99, settings.getInt("missing", 99));
    }

    @Test
    public void localSetAndGetLong() {
        AtomicInteger saveCalls = new AtomicInteger();
        PluginSettingsImpl settings = createSettings(saveCalls::incrementAndGet);

        settings.setLong("bigNum", 100000L);

        assertEquals(Optional.of(100000L), settings.getLong("bigNum"));
        assertEquals(100000L, settings.getLong("bigNum", -1L));
        assertEquals(1, saveCalls.get());
    }

    @Test
    public void localGetLongDefaultWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertFalse(settings.getLong("missing").isPresent());
        assertEquals(55L, settings.getLong("missing", 55L));
    }

    @Test
    public void localSetAndGetDouble() {
        AtomicInteger saveCalls = new AtomicInteger();
        PluginSettingsImpl settings = createSettings(saveCalls::incrementAndGet);

        settings.setDouble("ratio", 2.718);

        assertEquals(Optional.of(2.718), settings.getDouble("ratio"));
        assertEquals(2.718, settings.getDouble("ratio", -1.0), 0.0);
        assertEquals(1, saveCalls.get());
    }

    @Test
    public void localGetDoubleDefaultWhenMissing() {
        PluginSettingsImpl settings = createSettings();

        assertFalse(settings.getDouble("missing").isPresent());
        assertEquals(9.81, settings.getDouble("missing", 9.81), 0.0);
    }

    @Test
    public void localSetAndGetArray() {
        AtomicInteger saveCalls = new AtomicInteger();
        PluginSettingsImpl settings = createSettings(saveCalls::incrementAndGet);
        List<String> items = Arrays.asList("one", "two", "three");

        settings.setArray("list", items);

        assertEquals(items, settings.getArray("list"));
        assertEquals(items, settings.getArray("list", Collections.emptyList()));
        assertEquals(1, saveCalls.get());
    }

    @Test
    public void localGetArrayDefaultWhenMissing() {
        PluginSettingsImpl settings = createSettings();
        List<String> fallback = List.of("default");

        assertEquals(Collections.emptyList(), settings.getArray("missing"));
        assertEquals(fallback, settings.getArray("missing", fallback));
    }

    @Test
    public void mutatingPrefixedKeysIsRejected() {
        final PluginSettingsImpl settings = createSettings();

        expectIllegalArgument(() -> settings.setString(PREFIX + "key", "value"));
        expectIllegalArgument(() -> settings.remove(PREFIX + "key"));
        expectIllegalArgument(() -> settings.getSubSettings(PREFIX + "key"));
        expectIllegalArgument(() -> settings.setSubSettings(PREFIX + "key"));
    }

    @Test
    public void setBooleanWithPrefixIsRejected() {
        final PluginSettingsImpl settings = createSettings();

        expectIllegalArgument(() -> settings.setBoolean(PREFIX + "key", true));
    }

    @Test
    public void setIntWithPrefixIsRejected() {
        final PluginSettingsImpl settings = createSettings();

        expectIllegalArgument(() -> settings.setInt(PREFIX + "key", 1));
    }

    @Test
    public void setLongWithPrefixIsRejected() {
        final PluginSettingsImpl settings = createSettings();

        expectIllegalArgument(() -> settings.setLong(PREFIX + "key", 1L));
    }

    @Test
    public void setDoubleWithPrefixIsRejected() {
        final PluginSettingsImpl settings = createSettings();

        expectIllegalArgument(() -> settings.setDouble(PREFIX + "key", 1.0));
    }

    @Test
    public void setArrayWithPrefixIsRejected() {
        final PluginSettingsImpl settings = createSettings();

        expectIllegalArgument(() -> settings.setArray(PREFIX + "key", Collections.emptyList()));
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
