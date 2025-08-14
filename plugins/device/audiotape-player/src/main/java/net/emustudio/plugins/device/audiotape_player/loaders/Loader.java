/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player.loaders;

import net.emustudio.plugins.device.audiotape_player.TapePlaybackController;
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
    interface TapePlayback {

        void onFileStart();

        /**
         * Header block starts
         */
        void onHeaderStart();

        /**
         * Data block starts
         */
        void onDataStart();

        /**
         * Block flag (0 - header, 255- data)
         *
         * @param flag block flag
         */
        void onBlockFlag(int flag);

        /**
         * Raw data block. The type is determined by a previous onXXX call
         *
         * @param data data
         */
        void onBlockData(byte[] data);

        /**
         * On block checksum
         *
         * @param checksum block checksum
         */
        void onBlockChecksum(byte checksum);

        void onFileEnd();

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
         * On state change
         */
        void onStateChange(TapePlaybackController.CassetteState state);
    }

    void load(TapePlayback playback) throws IOException;
}
