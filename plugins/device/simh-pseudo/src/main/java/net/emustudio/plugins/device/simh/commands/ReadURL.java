/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.simh.commands;

import java.util.Arrays;

public class ReadURL implements Command {
    public final static ReadURL INS = new ReadURL();

    private final static int URL_MAX_LENGTH = 1024;

    private final char[] urlStore = new char[URL_MAX_LENGTH];
    private final char[] urlResult = new char[URL_MAX_LENGTH];
    private int urlPointer;
    private int resultLength;
    private int resultPointer;
    private boolean showAvailability;
    private boolean isInReadPhase;

    @Override
    public void reset() {
        urlPointer = 0;
        isInReadPhase = false;
    }

    @Override
    public byte read(Control control) {
        byte result = 0;
        if (isInReadPhase) {
            if (showAvailability) {
                if (resultPointer < resultLength)
                    result = 1;
                else {
                    Arrays.fill(urlResult, (char)0);
                    control.clearCommand();
                }
            } else if (resultPointer < resultLength) {
                result = (byte)urlResult[resultPointer++];
            }
            showAvailability = !showAvailability;
        } else {
            control.clearCommand();
        }
        return result;
    }

    @Override
    public void write(byte data, Control control) {
        if (isInReadPhase) {
            control.clearCommand();
        } else {
            if (data != 0) {
                if (urlPointer < URL_MAX_LENGTH - 1) {
                    urlStore[urlPointer++] = (char) (data & 0xff);
                }
            } else {
                urlStore[urlPointer] = 0;
                setURLContent();
                urlPointer = 0;
                resultPointer = 0;
                showAvailability = true;
                isInReadPhase = true;
            }
        }
    }

    @Override
    public void start(Control control) {
        reset();
    }


    private void setURLContent() {
        String str = "URL is not supported on this platform. START URL \"" +
            String.valueOf(urlStore, 0, urlPointer) + "\" URL END.";
        resultLength = Math.min(URL_MAX_LENGTH, str.length());
        System.arraycopy(str.toCharArray(), 0, urlResult, 0, resultLength);
    }
}
