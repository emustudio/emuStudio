/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.interaction;

import net.emustudio.emulib.runtime.helpers.SleepUtils;
import net.emustudio.plugins.device.adm3a.TerminalSettings;
import net.emustudio.plugins.device.adm3a.api.Keyboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Objects;

public class KeyboardFromFile extends Keyboard {
    private final static Logger LOGGER = LoggerFactory.getLogger(KeyboardFromFile.class);

    private final long delayNanos;
    private final Path inputFile;

    public KeyboardFromFile(TerminalSettings settings) {
        this.delayNanos = settings.getInputReadDelayMillis() * 1000000L;
        this.inputFile = Objects.requireNonNull(settings.getInputPath());
    }

    @Override
    public void process() {
        if (!inputFile.toFile().exists()) {
            LOGGER.warn("Input file {} does not exist", inputFile);
        } else {
            try (FileInputStream in = new FileInputStream(inputFile.toFile())) {
                int key;
                while ((key = in.read()) != -1) {
                    notifyOnKey((byte) key);
                    if (delayNanos > 0) {
                        SleepUtils.preciseSleepNanos(delayNanos);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Could not process input file", e);
            }
        }
    }
}
