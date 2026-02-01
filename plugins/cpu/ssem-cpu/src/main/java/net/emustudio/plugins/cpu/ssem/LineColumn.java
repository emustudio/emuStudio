/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.ssem;

import net.emustudio.emulib.runtime.ui.debugger.DebuggerColumn;

public class LineColumn implements DebuggerColumn<String> {
    private final static String LINE_FORMAT = "%04X";

    @Override
    public Class<String> getClassType() {
        return String.class;
    }

    @Override
    public String getTitle() {
        return "line";
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public void setValue(int location, Object value) {

    }

    @Override
    public String getValue(int location) {
        return String.format(LINE_FORMAT, location / 4);
    }
}
