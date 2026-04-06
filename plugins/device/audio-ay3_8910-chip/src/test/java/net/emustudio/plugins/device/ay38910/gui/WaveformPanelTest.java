/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ay38910.gui;

import net.emustudio.plugins.device.ay38910.Ay38910Chip;
import org.junit.Test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class WaveformPanelTest {

    @Test(timeout = 2_000)
    public void testPaintDoesNotBlockWhenChipStateIsLocked() throws Exception {
        Ay38910Chip chip = Ay38910Chip.silent();
        WaveformPanel panel = new WaveformPanel(chip);
        panel.setSize(320, 160);

        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);

        Thread locker = new Thread(() -> {
            synchronized (chip) {
                lockHeld.countDown();
                try {
                    releaseLock.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "ay38910-locker");
        locker.start();

        assertTrue(lockHeld.await(1, TimeUnit.SECONDS));

        Thread releaser = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            releaseLock.countDown();
        }, "ay38910-releaser");
        releaser.start();

        BufferedImage image = new BufferedImage(320, 160, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        long startedAt = System.nanoTime();
        try {
            panel.paint(graphics);
        } finally {
            graphics.dispose();
            releaseLock.countDown();
        }

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        locker.join(1_000);
        releaser.join(1_000);
        assertTrue("Painting should not wait for the chip monitor", elapsedMs < 250);
    }
}
