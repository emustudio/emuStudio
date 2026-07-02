/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation;

public interface EmulationProgress {
    EmulationProgress NONE = new EmulationProgress() {
        @Override
        public void show() {
        }

        @Override
        public void setAction(String msg, boolean stopEnabled) {
        }

        @Override
        public void dispose() {
        }
    };

    void show();

    void setAction(String msg, boolean stopEnabled);

    void dispose();
}
