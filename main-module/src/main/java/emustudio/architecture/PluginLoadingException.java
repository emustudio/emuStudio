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
package emustudio.architecture;

import emulib.plugins.Plugin;

/**
 * This exception is thrown when a plugin could not be loaded.
 *
 */
public class PluginLoadingException extends Exception {
    private final Plugin source;
    private final String pluginName;

    public PluginLoadingException(String message, String pluginName,
            Plugin source) {
        super(message);
        this.pluginName = pluginName;
        this.source = source;
    }

    public Plugin getSource() {
        return source;
    }

    public String getPluginName() {
        return pluginName;
    }
}
