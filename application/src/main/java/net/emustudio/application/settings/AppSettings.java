/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.emustudio.emulib.runtime.settings.PluginSettings;

import java.nio.file.Path;
import java.util.Optional;

import static net.emustudio.emulib.runtime.settings.PluginSettings.*;


@SuppressWarnings("unused")
public class AppSettings extends PluginSettingsImpl {
    public final static String KEY_NOGUI = EMUSTUDIO_NO_GUI.substring(EMUSTUDIO_PREFIX.length());
    public final static String KEY_AUTO = EMUSTUDIO_AUTO.substring(EMUSTUDIO_PREFIX.length());
    public final static String KEY_USE_SCHEMA_GRID = "useSchemaGrid";
    public final static String KEY_SCHEMA_GRID_GAP = "schemaGridGap";
    public final static String KEY_THEME = "theme";

    private final static int DEFAULT_GRID_GAP = 20;

    public transient final boolean emuStudioAuto;
    public transient final boolean noGUI;

    public enum Theme {
        LIGHT,
        DARK,
        INTELLIJ
    }

    public AppSettings(Config config, boolean nogui, boolean auto) {
        super(config, PluginSettings.UNAVAILABLE, () -> {});
        this.emuStudioAuto = auto;
        this.noGUI = nogui;
    }

    public static AppSettings fromFile(Path file, boolean nogui, boolean auto) {
        FileConfig config = FileConfig.builder(file).autosave().sync().build();
        config.load();
        return new AppSettings(config, nogui, auto);
    }

    public boolean useSchemaGrid() {
        return getBoolean(KEY_USE_SCHEMA_GRID).orElse(true);
    }

    public void setUseSchemaGrid(boolean useGrid) {
        set(KEY_USE_SCHEMA_GRID, useGrid);
    }

    public int getSchemaGridGap() {
        return getInt(KEY_SCHEMA_GRID_GAP).orElse(DEFAULT_GRID_GAP);
    }

    public void setSchemaGridGap(int value) {
        set(KEY_SCHEMA_GRID_GAP, value);
    }

    public Optional<Theme> getTheme() {
        return getString(KEY_THEME).map(Theme::valueOf);
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        if (KEY_AUTO.equals(key)) {
            return Optional.of(emuStudioAuto);
        } else if (KEY_NOGUI.equals(key)) {
            return Optional.of(noGUI);
        }
        return super.getBoolean(key);
    }
}
