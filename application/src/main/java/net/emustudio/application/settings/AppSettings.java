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
import com.electronwill.nightconfig.core.file.FileConfig;

import java.nio.file.Path;
import java.util.Optional;

import static net.emustudio.emulib.runtime.settings.PluginSettings.*;


@SuppressWarnings("unused")
public class AppSettings extends BasicSettingsImpl {
    public final static String KEY_NOGUI = EMUSTUDIO_NO_GUI.substring(EMUSTUDIO_PREFIX.length());
    public final static String KEY_AUTO = EMUSTUDIO_AUTO.substring(EMUSTUDIO_PREFIX.length());
    public final static String KEY_USE_SCHEMA_GRID = "useSchemaGrid";
    public final static String KEY_SCHEMA_GRID_GAP = "schemaGridGap";
    public final static String KEY_LOOK_AND_FEEL = "lookAndFeel";

    private final static int DEFAULT_GRID_GAP = 20;

    public transient final boolean emuStudioAuto;
    public transient final boolean noGUI;

    public AppSettings(Config config, boolean nogui, boolean auto) {
        super(config, System.out::println);
        this.emuStudioAuto = auto;
        this.noGUI = nogui;
    }

    @Override
    public boolean contains(String key) {
        return KEY_NOGUI.equals(key) || KEY_AUTO.equals(key) || super.contains(key);
    }

    public boolean useSchemaGrid() {
        return getBoolean(KEY_USE_SCHEMA_GRID, true);
    }

    public void setUseSchemaGrid(boolean useGrid) {
        setBoolean(KEY_USE_SCHEMA_GRID, useGrid);
    }

    public int getSchemaGridGap() {
        return getInt(KEY_SCHEMA_GRID_GAP, DEFAULT_GRID_GAP);
    }

    public void setSchemaGridGap(int value) {
        setInt(KEY_SCHEMA_GRID_GAP, value);
    }

    public Optional<String> getLookAndFeel() {
        return getString(KEY_LOOK_AND_FEEL);
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

    public static AppSettings fromFile(Path file, boolean nogui, boolean auto) {
        FileConfig config = FileConfig.builder(file).autosave().concurrent().sync().build();
        config.load();
        return new AppSettings(config, nogui, auto);
    }
}
