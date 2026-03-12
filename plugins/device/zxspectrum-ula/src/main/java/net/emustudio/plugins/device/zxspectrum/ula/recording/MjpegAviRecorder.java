/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.recording;

import net.emustudio.plugins.device.zxspectrum.ula.audio.Beeper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Incremental Motion JPEG AVI writer with stereo PCM audio.
 *
 * <p>Frames and PCM chunks are written directly into a temporary AVI file while recording is active,
 * so the display window does not need to keep the whole capture in memory. On stop, the writer appends
 * the legacy {@code idx1} index, patches the AVI headers, and the completed temporary file can then be
 * moved to a user-selected destination.
 */
public final class MjpegAviRecorder implements AutoCloseable {
    private static final long MAX_CLASSIC_AVI_SIZE = 0xFFFF_FFFFL;
    private static final int AVI_HAS_INDEX = 0x10;
    private static final int AVIIF_KEYFRAME = 0x10;
    private static final int PCM_FORMAT = 1;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int AUDIO_BLOCK_ALIGN = Beeper.CHANNELS * (BITS_PER_SAMPLE / 8);
    private static final int AVIH_SIZE = 56;
    private static final int STRH_SIZE = 56;
    private static final int BITMAPINFOHEADER_SIZE = 40;
    private static final int WAVE_FORMAT_EX_SIZE = 16;
    private static final int VIDEO_STRL_SIZE = 4 + (8 + STRH_SIZE) + (8 + BITMAPINFOHEADER_SIZE);
    private static final int AUDIO_STRL_SIZE = 4 + (8 + STRH_SIZE) + (8 + WAVE_FORMAT_EX_SIZE);
    private static final int HDRL_SIZE = 4 + (8 + AVIH_SIZE) + (8 + VIDEO_STRL_SIZE) + (8 + AUDIO_STRL_SIZE);
    private static final String VIDEO_CHUNK_ID = "00dc";
    private static final String AUDIO_CHUNK_ID = "01wb";

    private final Path tempFile;
    private final RandomAccessFile output;
    private final int width;
    private final int height;
    private final int videoScale;
    private final int videoRate;
    private final int audioSampleRate;
    private final List<IndexEntry> indexEntries = new ArrayList<>();
    private final ReusableByteArrayOutputStream jpegBuffer = new ReusableByteArrayOutputStream(64 * 1024);

    private long riffSizeOffset;
    private long avihOffset;
    private long videoStrhOffset;
    private long audioStrhOffset;
    private long moviSizeOffset;
    private long moviTypeOffset;
    private long audioBytesWritten;
    private int videoFramesWritten;
    private int maxVideoChunkSize;
    private int maxAudioChunkSize;
    private boolean closed;
    private IOException failure;

    public MjpegAviRecorder(int width, int height, int videoScale, int videoRate, int audioSampleRate) throws IOException {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Recording dimensions must be > 0");
        }
        if (videoScale <= 0 || videoRate <= 0) {
            throw new IllegalArgumentException("Video timing must be > 0");
        }
        if (audioSampleRate <= 0) {
            throw new IllegalArgumentException("Audio sample rate must be > 0");
        }
        this.width = width;
        this.height = height;
        this.videoScale = videoScale;
        this.videoRate = videoRate;
        this.audioSampleRate = audioSampleRate;
        this.tempFile = Files.createTempFile("emustudio-zxspectrum-recording-", ".avi");
        this.output = new RandomAccessFile(tempFile.toFile(), "rw");
        writeHeaders();
    }

    public synchronized void recordVideoFrame(BufferedImage frame) {
        if (!isWritable()) {
            return;
        }
        try {
            jpegBuffer.reset();
            if (!ImageIO.write(frame, "jpg", jpegBuffer)) {
                throw new IOException("No JPEG encoder is available");
            }
            int size = jpegBuffer.size();
            maxVideoChunkSize = Math.max(maxVideoChunkSize, size);
            writeChunk(VIDEO_CHUNK_ID, jpegBuffer.buffer(), size, AVIIF_KEYFRAME);
            videoFramesWritten++;
        } catch (IOException e) {
            failure = e;
        }
    }

    public synchronized void recordAudio(byte[] pcmFrames, int length) {
        if (!isWritable() || length <= 0) {
            return;
        }
        try {
            maxAudioChunkSize = Math.max(maxAudioChunkSize, length);
            writeChunk(AUDIO_CHUNK_ID, pcmFrames, length, 0);
            audioBytesWritten += length;
        } catch (IOException e) {
            failure = e;
        }
    }

    public synchronized Path moveTo(Path target) throws IOException {
        close();
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        try {
            return Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            Files.copy(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(tempFile);
            return target;
        }
    }

    public synchronized void discard() throws IOException {
        if (!closed) {
            output.close();
            closed = true;
        }
        Files.deleteIfExists(tempFile);
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            if (failure != null) {
                throw failure;
            }
            return;
        }

        IOException closeError = null;
        try {
            if (failure == null) {
                finalizeFile();
            }
        } catch (IOException e) {
            closeError = e;
        }

        try {
            output.close();
        } catch (IOException e) {
            if (closeError == null) {
                closeError = e;
            }
        } finally {
            closed = true;
        }

        if (failure != null) {
            throw failure;
        }
        if (closeError != null) {
            throw closeError;
        }
    }

    private boolean isWritable() {
        return !closed && failure == null;
    }

    private void writeHeaders() throws IOException {
        writeFourCc("RIFF");
        riffSizeOffset = output.getFilePointer();
        writeInt(0);
        writeFourCc("AVI ");

        writeFourCc("LIST");
        writeInt(HDRL_SIZE);
        writeFourCc("hdrl");

        writeFourCc("avih");
        writeInt(AVIH_SIZE);
        avihOffset = output.getFilePointer();
        writeZeroBytes(AVIH_SIZE);

        writeFourCc("LIST");
        writeInt(VIDEO_STRL_SIZE);
        writeFourCc("strl");

        writeFourCc("strh");
        writeInt(STRH_SIZE);
        videoStrhOffset = output.getFilePointer();
        writeZeroBytes(STRH_SIZE);

        writeFourCc("strf");
        writeInt(BITMAPINFOHEADER_SIZE);
        writeBitmapInfoHeader();

        writeFourCc("LIST");
        writeInt(AUDIO_STRL_SIZE);
        writeFourCc("strl");

        writeFourCc("strh");
        writeInt(STRH_SIZE);
        audioStrhOffset = output.getFilePointer();
        writeZeroBytes(STRH_SIZE);

        writeFourCc("strf");
        writeInt(WAVE_FORMAT_EX_SIZE);
        writeWaveFormat();

        writeFourCc("LIST");
        moviSizeOffset = output.getFilePointer();
        writeInt(0);
        moviTypeOffset = output.getFilePointer();
        writeFourCc("movi");
    }

    private void writeBitmapInfoHeader() throws IOException {
        writeInt(BITMAPINFOHEADER_SIZE);
        writeInt(width);
        writeInt(height);
        writeShort(1);
        writeShort(24);
        writeFourCc("MJPG");
        writeInt(width * height * 3);
        writeInt(0);
        writeInt(0);
        writeInt(0);
        writeInt(0);
    }

    private void writeWaveFormat() throws IOException {
        writeShort(PCM_FORMAT);
        writeShort(Beeper.CHANNELS);
        writeInt(audioSampleRate);
        writeInt(audioSampleRate * AUDIO_BLOCK_ALIGN);
        writeShort(AUDIO_BLOCK_ALIGN);
        writeShort(BITS_PER_SAMPLE);
    }

    private void writeChunk(String chunkId, byte[] data, int length, int flags) throws IOException {
        long chunkBytes = 8L + length + (length & 1);
        long projectedIndexBytes = 8L + (indexEntries.size() + 1L) * 16L;
        if (output.getFilePointer() + chunkBytes + projectedIndexBytes > MAX_CLASSIC_AVI_SIZE) {
            throw new IOException("Recording is too large for classic AVI output");
        }
        long chunkOffset = output.getFilePointer();
        writeFourCc(chunkId);
        writeInt(length);
        output.write(data, 0, length);
        if ((length & 1) != 0) {
            output.write(0);
        }
        indexEntries.add(new IndexEntry(chunkId, flags, chunkOffset - moviTypeOffset, length));
    }

    private void finalizeFile() throws IOException {
        long idx1Offset = output.getFilePointer();
        writeFourCc("idx1");
        writeInt(indexEntries.size() * 16);
        for (IndexEntry indexEntry : indexEntries) {
            writeFourCc(indexEntry.chunkId);
            writeInt(indexEntry.flags);
            writeInt((int) indexEntry.offset);
            writeInt(indexEntry.size);
        }

        long fileLength = output.getFilePointer();
        output.seek(riffSizeOffset);
        writeInt((int) (fileLength - 8));

        output.seek(moviSizeOffset);
        writeInt((int) (idx1Offset - moviTypeOffset));

        patchAviMainHeader();
        patchVideoStreamHeader();
        patchAudioStreamHeader();

        output.seek(fileLength);
    }

    private void patchAviMainHeader() throws IOException {
        output.seek(avihOffset);
        writeInt((int) Math.round(videoScale * 1_000_000.0 / videoRate));
        writeInt(computeMaxBytesPerSecond());
        writeInt(0);
        writeInt(AVI_HAS_INDEX);
        writeInt(videoFramesWritten);
        writeInt(0);
        writeInt(2);
        writeInt(Math.max(maxVideoChunkSize, maxAudioChunkSize));
        writeInt(width);
        writeInt(height);
        writeInt(0);
        writeInt(0);
        writeInt(0);
        writeInt(0);
    }

    private void patchVideoStreamHeader() throws IOException {
        output.seek(videoStrhOffset);
        writeFourCc("vids");
        writeFourCc("MJPG");
        writeInt(0);
        writeShort(0);
        writeShort(0);
        writeInt(0);
        writeInt(videoScale);
        writeInt(videoRate);
        writeInt(0);
        writeInt(videoFramesWritten);
        writeInt(maxVideoChunkSize);
        writeInt(-1);
        writeInt(0);
        writeShort(0);
        writeShort(0);
        writeShort(width);
        writeShort(height);
    }

    private void patchAudioStreamHeader() throws IOException {
        output.seek(audioStrhOffset);
        writeFourCc("auds");
        writeInt(0);
        writeInt(0);
        writeShort(0);
        writeShort(0);
        writeInt(0);
        writeInt(AUDIO_BLOCK_ALIGN);
        writeInt(audioSampleRate * AUDIO_BLOCK_ALIGN);
        writeInt(0);
        writeInt((int) (audioBytesWritten / AUDIO_BLOCK_ALIGN));
        writeInt(maxAudioChunkSize);
        writeInt(-1);
        writeInt(AUDIO_BLOCK_ALIGN);
        writeShort(0);
        writeShort(0);
        writeShort(0);
        writeShort(0);
    }

    private int computeMaxBytesPerSecond() {
        long videoBytesPerSecond = (long) Math.ceil(maxVideoChunkSize * (videoRate / (double) videoScale));
        long audioBytesPerSecond = (long) audioSampleRate * AUDIO_BLOCK_ALIGN;
        long total = videoBytesPerSecond + audioBytesPerSecond;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(total, 1));
    }

    private void writeZeroBytes(int count) throws IOException {
        output.write(new byte[count]);
    }

    private void writeFourCc(String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeInt(int value) throws IOException {
        output.write(value & 0xFF);
        output.write((value >>> 8) & 0xFF);
        output.write((value >>> 16) & 0xFF);
        output.write((value >>> 24) & 0xFF);
    }

    private void writeShort(int value) throws IOException {
        output.write(value & 0xFF);
        output.write((value >>> 8) & 0xFF);
    }

    private static final class IndexEntry {
        private final String chunkId;
        private final int flags;
        private final long offset;
        private final int size;

        private IndexEntry(String chunkId, int flags, long offset, int size) {
            this.chunkId = chunkId;
            this.flags = flags;
            this.offset = offset;
            this.size = size;
        }
    }

    private static final class ReusableByteArrayOutputStream extends ByteArrayOutputStream {
        private ReusableByteArrayOutputStream(int size) {
            super(size);
        }

        private byte[] buffer() {
            return buf;
        }
    }
}
