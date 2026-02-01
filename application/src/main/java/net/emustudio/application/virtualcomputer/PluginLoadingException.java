/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.virtualcomputer;

import net.emustudio.emulib.plugins.Plugin;

/**
 * This exception is thrown when a plugin could not be loaded.
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
