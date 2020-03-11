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
import java.util.*;

@SuppressWarnings("unused")
public class ApplicationConfig implements Closeable {
    public transient final boolean emuStudioAuto;
    public transient final boolean noGUI;

    private final FileConfig config;

    public ApplicationConfig(FileConfig config, boolean nogui, boolean auto) {
        this.config = Objects.requireNonNull(config);
        this.emuStudioAuto = auto;
        this.noGUI = nogui;
    }

    public boolean contains(String key) {
        switch (key) {
            case "useSchemaGrid":
            case "auto":
            case "nogui":
            case "schemaGridGap":
                return true;
        }
        return false;
    }

    public Optional<Boolean> getBoolean(String key) {
        switch (key) {
            case "useSchemaGrid": return useSchemaGrid();
            case "auto": return Optional.of(emuStudioAuto);
            case "nogui": return Optional.of(noGUI);
        }
        return Optional.empty();
    }

    public Optional<Integer> getInt(String key) {
        if ("schemaGridGap".equals(key)) {
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
        return Optional.empty();
    }

    public List<String> getArray(String key) {
        return Collections.emptyList();
    }

    public Optional<Boolean> useSchemaGrid() {
        return config.getOptional("useSchemaGrid");
    }

    public void setUseSchemaGrid(boolean useSchemaGrid) {
        config.set("useSchemaGrid", useSchemaGrid);
    }

    public Optional<Integer> getSchemaGridGap() {
        return config.getOptional("schemaGridGap");
    }

    public void setSchemaGridGap(int schemaGridGap) {
        config.set("schemaGridGap", schemaGridGap);
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
