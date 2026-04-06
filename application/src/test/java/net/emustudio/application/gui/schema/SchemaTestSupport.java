/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.schema;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.emustudio.application.gui.components.P;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.settings.PluginConfig;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import org.junit.rules.TemporaryFolder;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public final class SchemaTestSupport {
    private SchemaTestSupport() {
    }

    public static ComputerConfig createComputerConfig(TemporaryFolder temporaryFolder, String name) throws IOException {
        FileConfig config = FileConfig.of(temporaryFolder.newFile(name.replace(' ', '_') + ".toml"));
        config.set("name", name);
        return new ComputerConfig(config);
    }

    public static AppSettings createAppSettings(boolean useGrid, int gridGap) {
        AppSettings settings = new AppSettings(Config.inMemory(), false, false);
        settings.setUseSchemaGrid(useGrid);
        settings.setSchemaGridGap(gridGap);
        return settings;
    }

    public static PluginConfig pluginConfig(String id, PLUGIN_TYPE pluginType, String pluginFile, int x, int y) {
        return PluginConfig.create(id, pluginType, id, pluginFile, P.of(x, y), Config.inMemory());
    }

    public static Graphics2D createGraphics() {
        return new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB).createGraphics();
    }

    public static MouseEvent mouseEvent(Component source, int id, int button, int x, int y) {
        return new MouseEvent(source, id, System.currentTimeMillis(), 0, x, y, 1, false, button);
    }

    public static <T> T getField(Object target, String fieldName, Class<T> type) throws Exception {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return type.cast(field.get(target));
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
