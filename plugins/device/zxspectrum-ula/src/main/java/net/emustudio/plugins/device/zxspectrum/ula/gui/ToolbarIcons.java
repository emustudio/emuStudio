/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import javax.swing.*;
import java.util.Objects;

/**
 * Toolbar icons for the ZX Spectrum display window, loaded from 22×22 PNG resources.
 */
final class ToolbarIcons {

    private ToolbarIcons() {
    }

    static Icon keyboard() {
        return loadIcon("toolbar-keyboard.png");
    }

    static Icon volume() {
        return loadIcon("toolbar-volume.png");
    }

    static Icon record() {
        return loadIcon("toolbar-record.png");
    }

    static Icon stop() {
        return loadIcon("toolbar-stop.png");
    }

    private static Icon loadIcon(String name) {
        return new ImageIcon(Objects.requireNonNull(
                ToolbarIcons.class.getResource(name),
                "Missing toolbar icon resource: " + name
        ));
    }
}
