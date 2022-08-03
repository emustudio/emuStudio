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
package net.emustudio.plugins.device.mits88sio.settings;

import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class SioDeviceSettings {
    private final boolean guiSupported;
    private final SioUnitSettings sioUnit;

    public SioDeviceSettings(PluginSettings settings) {
        this.guiSupported = !settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        this.sioUnit = new SioUnitSettings(settings);
    }

    public boolean isGuiSupported() {
        return guiSupported;
    }

    public SioUnitSettings getSioUnit() {
        return sioUnit;
    }
}
