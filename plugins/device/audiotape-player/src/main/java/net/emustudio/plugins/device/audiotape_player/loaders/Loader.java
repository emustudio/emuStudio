/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player.loaders;

import net.emustudio.plugins.device.audiotape_player.TapePlaybackController;
import net.jcip.annotations.ThreadSafe;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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

        /**
         * TZX Block 0x11 - Turbo Speed Data Block.
         * Like standard speed data but with custom timing parameters.
         *
         * @param pilotPulseLen      length of pilot pulse in T-states
         * @param sync1PulseLen      length of first sync pulse in T-states
         * @param sync2PulseLen      length of second sync pulse in T-states
         * @param zeroBitPulseLen    length of zero bit pulse in T-states
         * @param oneBitPulseLen     length of one bit pulse in T-states
         * @param pilotToneCount     number of pilot pulses
         * @param usedBitsInLastByte used bits in the last byte (1-8)
         * @param pauseAfterMs       pause after this block in ms
         * @param data               data bytes (including flag + data + checksum, as in TAP)
         */
        default void onTurboSpeedData(int pilotPulseLen, int sync1PulseLen, int sync2PulseLen,
                                      int zeroBitPulseLen, int oneBitPulseLen, int pilotToneCount,
                                      int usedBitsInLastByte, int pauseAfterMs, byte[] data) {
        }

        /**
         * TZX Block 0x12 - Pure Tone.
         * Generates a sequence of pulses of the same length.
         *
         * @param pulseLength length of each pulse in T-states
         * @param pulseCount  number of pulses
         */
        default void onPureTone(int pulseLength, int pulseCount) {
        }

        /**
         * TZX Block 0x13 - Pulse Sequence.
         * Generates a sequence of pulses of different lengths.
         *
         * @param pulseLengths array of pulse lengths in T-states
         */
        default void onPulseSequence(int[] pulseLengths) {
        }

        /**
         * TZX Block 0x14 - Pure Data Block.
         * Data block without pilot tone or sync pulses.
         *
         * @param zeroBitPulseLen    length of zero bit pulse in T-states
         * @param oneBitPulseLen     length of one bit pulse in T-states
         * @param usedBitsInLastByte used bits in the last byte (1-8)
         * @param pauseAfterMs       pause after this block in ms
         * @param data               raw data bytes
         */
        default void onPureData(int zeroBitPulseLen, int oneBitPulseLen,
                                int usedBitsInLastByte, int pauseAfterMs, byte[] data) {
        }

        /**
         * TZX Block 0x15 - Direct Recording.
         * Raw sample data at a given sample rate.
         *
         * @param tstatesPerSample   T-states per sample
         * @param pauseAfterMs       pause after this block in ms
         * @param usedBitsInLastByte used bits in the last byte (1-8)
         * @param samples            raw sample data (each bit = one sample)
         */
        default void onDirectRecording(int tstatesPerSample, int pauseAfterMs,
                                       int usedBitsInLastByte, byte[] samples) {
        }

        /**
         * TZX Block 0x18 - CSW Recording.
         *
         * @param pauseAfterMs     pause after this block in ms
         * @param sampleRate       sample rate in Hz
         * @param compressionType  1=RLE, 2=Z-RLE
         * @param storedPulseCount number of stored pulses (after decompression)
         * @param data             compressed pulse data
         */
        default void onCswRecording(int pauseAfterMs, int sampleRate, int compressionType,
                                    long storedPulseCount, byte[] data) {
        }

        /**
         * TZX Block 0x19 - Generalized Data Block.
         *
         * @param pauseAfterMs pause after this block in ms
         * @param data         block data (symbol tables + pilot/data streams)
         */
        default void onGeneralizedData(int pauseAfterMs, byte[] data) {
        }

        /**
         * TZX Block 0x20 - Pause (Silence) or Stop the Tape.
         * If durationMs is 0, it means "stop the tape" and wait for user to resume.
         *
         * @param durationMs duration of silence in ms (0 = stop the tape)
         */
        default void onPause(int durationMs) {
        }

        /**
         * TZX Block 0x21 - Group Start.
         *
         * @param name group name
         */
        default void onGroupStart(String name) {
        }

        /**
         * TZX Block 0x22 - Group End.
         */
        default void onGroupEnd() {
        }

        /**
         * TZX Block 0x2A - Stop the tape if in 48K mode.
         */
        default void onStopIfIn48KMode() {
        }

        /**
         * TZX Block 0x2B - Set Signal Level.
         *
         * @param level signal level (0=low, 1=high)
         */
        default void onSetSignalLevel(int level) {
        }

        /**
         * TZX Block 0x30 - Text Description.
         *
         * @param text description text
         */
        default void onTextDescription(String text) {
        }

        /**
         * TZX Block 0x31 - Message Block.
         *
         * @param message             message text
         * @param displayTimeSeconds  display time in seconds
         */
        default void onMessage(String message, int displayTimeSeconds) {
        }

        /**
         * TZX Block 0x32 - Archive Info.
         *
         * @param entries list of [type_description, value] pairs
         */
        default void onArchiveInfo(List<String[]> entries) {
        }

        /**
         * TZX Block 0x33 - Hardware Type.
         *
         * @param entries list of [hwType, hwId, hwInfo] entries
         */
        default void onHardwareInfo(List<int[]> entries) {
        }

        /**
         * TZX Block 0x35 - Custom Info Block.
         *
         * @param id   identification string (up to 16 chars)
         * @param data custom info data
         */
        default void onCustomInfo(String id, byte[] data) {
        }

        /**
         * TZX Block 0x5A - Glue Block.
         * Used to merge multiple TZX files together. Should be skipped by players.
         */
        default void onGlueBlock() {
        }
    }

    void load(TapePlayback playback) throws IOException;
}
