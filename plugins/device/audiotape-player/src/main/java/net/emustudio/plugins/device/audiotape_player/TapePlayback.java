/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player;

import net.emustudio.emulib.runtime.io.TapeListener;

/** Plugin-owned cassette state in addition to shared tape callbacks. */
public interface TapePlayback extends TapeListener {
    void onStateChange(TapePlaybackController.CassetteState state);
}
