/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd;

import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

public class Resources {

    public static Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.device.mits88dcdd.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }

    public static String getVersion() {
        return getResourceBundle().map(b -> b.getString("version")).orElse("(unknown)");
    }

    public static String getCopyright() {
        return getResourceBundle().map(b -> b.getString("copyright")).orElse("(unknown)");
    }
}
