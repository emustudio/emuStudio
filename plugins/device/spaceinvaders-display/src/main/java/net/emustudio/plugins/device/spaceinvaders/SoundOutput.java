/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

interface SoundOutput {
    // Ordinals match the standard Space Invaders sample filenames, 0.wav through 9.wav.
    enum Sample {
        UFO, SHOT, PLAYER_HIT, INVADER_HIT, FLEET_1, FLEET_2, FLEET_3, FLEET_4, UFO_HIT, BONUS
    }

    SoundOutput SILENT = new SoundOutput() {
        public void play(Sample sample, boolean loop) { }
        public void stop(Sample sample) { }
        public void stopAll() { }
    };

    void play(Sample sample, boolean loop);
    void stop(Sample sample);
    void stopAll();
}
