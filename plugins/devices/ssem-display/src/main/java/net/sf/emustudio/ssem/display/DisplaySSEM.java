/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.ssem.display;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.device.AbstractDevice;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import emulib.runtime.exceptions.PluginInitializationException;

import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginType(
        type = PLUGIN_TYPE.DEVICE,
        title = "SSEM CRT display",
        copyright = "\u00A9 Copyright 2006-2017, Peter Jakubčo",
        description = "CRT display for SSEM computer."
)
@SuppressWarnings("unused")
public class DisplaySSEM extends AbstractDevice {
    private boolean nogui;
    private final ContextPool contextPool;
    private Optional<DisplayDialog> display = Optional.empty();

    public DisplaySSEM(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.ssem.display.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        super.initialize(settings);
        MemoryContext<Byte> memory = contextPool.getMemoryContext(pluginID, MemoryContext.class);
        
        if (memory.getDataType() != Byte.class) {
            throw new PluginInitializationException(this, "Expected Byte memory cell type!");
        }

        String s = settings.readSetting(pluginID, SettingsManager.NO_GUI);
        nogui = (s != null) && s.toUpperCase().equals("TRUE");

        if (!nogui) {
            display = Optional.of(new DisplayDialog(memory));
        }
    }

    @Override
    public void reset() {
        display.ifPresent(DisplayDialog::reset);
    }

    @Override
    public void destroy() {
        display.ifPresent(DisplayDialog::dispose);
    }

    @Override
    public void showGUI() {
        display.ifPresent(displayDialog -> displayDialog.setVisible(true));
    }

    @Override
    public void showSettings() {
        // we don't have settings GUI
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }
}
