/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.application.configuration;

import com.electronwill.nightconfig.core.file.FileConfig;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.emulib.runtime.PluginSettings.*;

@SuppressWarnings("unused")
public class ApplicationConfig implements Closeable {
    public final static String KEY_NOGUI = EMUSTUDIO_NO_GUI.substring(EMUSTUDIO_PREFIX.length());
    public final static String KEY_AUTO = EMUSTUDIO_AUTO.substring(EMUSTUDIO_PREFIX.length());
    public final static String KEY_USE_SCHEMA_GRID = "useSchemaGrid";
    public final static String KEY_SCHEMA_GRID_GAP = "schemaGridGap";
    public final static String KEY_LOOK_AND_FEEL = "lookAndFeel";

    public transient final boolean emuStudioAuto;
    public transient final boolean noGUI;

    private final FileConfig config;

    public ApplicationConfig(FileConfig config, boolean nogui, boolean auto) {
        this.config = Objects.requireNonNull(config);
        this.emuStudioAuto = auto;
        this.noGUI = nogui;
    }

    public boolean contains(String key) {
        return KEY_NOGUI.equals(key) || KEY_AUTO.equals(key) || KEY_USE_SCHEMA_GRID.equals(key)
            || KEY_SCHEMA_GRID_GAP.equals(key) || KEY_LOOK_AND_FEEL.equals(key);
    }

    public Optional<Boolean> getBoolean(String key) {
        if (KEY_USE_SCHEMA_GRID.equals(key)) {
            return useSchemaGrid();
        } else if (KEY_SCHEMA_GRID_GAP.equals(key)) {
            return useSchemaGrid();
        } else if (KEY_AUTO.equals(key)) {
            return Optional.of(emuStudioAuto);
        } else if (KEY_NOGUI.equals(key)) {
            return Optional.of(noGUI);
        }
        return Optional.empty();
    }

    public Optional<Integer> getInt(String key) {
        if (KEY_SCHEMA_GRID_GAP.equals(key)) {
            return getSchemaGridGap();
        }
        return Optional.empty();
    }

    public Optional<Long> getLong(String key) {
        return Optional.empty();
    }

    public Optional<Double> getDouble(String key) {
        return Optional.empty();
    }

    public Optional<String> getString(String key) {
        if (KEY_LOOK_AND_FEEL.equals(key)) {
            return getLookAndFeel();
        }
        return Optional.empty();
    }

    public List<String> getArray(String key) {
        return Collections.emptyList();
    }

    public Optional<Boolean> useSchemaGrid() {
        return config.getOptional(KEY_USE_SCHEMA_GRID);
    }

    public void setUseSchemaGrid(boolean useSchemaGrid) {
        config.set(KEY_USE_SCHEMA_GRID, useSchemaGrid);
    }

    public Optional<Integer> getSchemaGridGap() {
        return config.getOptional(KEY_SCHEMA_GRID_GAP);
    }

    public void setSchemaGridGap(int schemaGridGap) {
        config.set(KEY_SCHEMA_GRID_GAP, schemaGridGap);
    }

    public Optional<String> getLookAndFeel() {
        return config.getOptional(KEY_LOOK_AND_FEEL);
    }

    public void setLookAndFeel(String lookAndFeel) {
        config.set(KEY_LOOK_AND_FEEL, lookAndFeel);
    }

    public void save() {
        config.save();
    }

    @Override
    public void close() {
        config.close();
    }

    public static ApplicationConfig fromFile(Path file, boolean nogui, boolean auto) {
        FileConfig config = FileConfig.of(file);
        config.load();
        return new ApplicationConfig(config, nogui, auto);
    }
}
