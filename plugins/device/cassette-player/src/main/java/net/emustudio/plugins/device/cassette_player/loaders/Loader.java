/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.device.cassette_player.loaders;

import net.emustudio.plugins.device.cassette_player.CassetteController;
import net.jcip.annotations.ThreadSafe;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public interface Loader {

    Map<String, Function<Path, Loader>> LOADERS = Map.of(
            "tap", TapLoader::new,
            "tzx", TzxLoader::new
    );


    static boolean hasLoader(Path path) {
        int index = path.toString().lastIndexOf(".");
        String extension = (index == -1) ?
                "" : path.toString().substring(index + 1).toLowerCase(Locale.ENGLISH);

        return LOADERS
                .entrySet()
                .stream()
                .anyMatch(l -> l.getKey().equals(extension));
    }

    static Optional<Loader> create(Path path) {
        int index = path.toString().lastIndexOf(".");
        String extension = (index == -1) ?
                "" : path.toString().substring(index + 1).toLowerCase(Locale.ENGLISH);

        return LOADERS
                .entrySet()
                .stream()
                .filter(l -> l.getKey().equals(extension))
                .findFirst()
                .map(Map.Entry::getValue)
                .map(l -> l.apply(path));
    }

    @ThreadSafe
    interface PlaybackListener {

        // tzx version

        /**
         * Data block will be a program in BASIC.
         *
         * @param fileName      BASIC file name
         * @param dataLength    data block length
         * @param autoStart     line number
         * @param programLength program length = start of variable area
         */
        void onProgram(String fileName, int dataLength, int autoStart, int programLength);

        /**
         * Data block will be a number array variable
         *
         * @param filename   BASIC file name
         * @param dataLength data block length
         * @param variable   variable name 'A'..'Z'
         */
        void onNumberArray(String filename, int dataLength, char variable);

        /**
         * Data block will be a String array variable
         *
         * @param filename   BASIC file name
         * @param dataLength data block length
         * @param variable   variable name 'A'..'Z'
         */
        void onStringArray(String filename, int dataLength, char variable);

        /**
         * Data block will be a memory block
         *
         * @param filename     BASIC file name
         * @param dataLength   data block length
         * @param startAddress starting address in memory
         */
        void onMemoryBlock(String filename, int dataLength, int startAddress);

        /**
         * Raw data block. The type is determined by a previous onXXX call
         *
         * @param data data
         */
        void onData(byte[] data);

        /**
         * Pause given milliseconds
         *
         * @param millis milliseconds to pause
         */
        void onPause(int millis);

        /**
         * On state change
         */
        void onStateChange(CassetteController.CassetteState state);
    }

    void load(PlaybackListener listener) throws IOException;
}
