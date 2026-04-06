/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.recording;

import net.emustudio.plugins.device.zxspectrum.ula.audio.Beeper;
import net.jcip.annotations.NotThreadSafe;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.demuxer.MP4DemuxerTrack;
import org.jcodec.containers.mp4.muxer.CodecMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mp4.muxer.PCMMP4MuxerTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * One-time usable Video+Audio recorder.
 *
 * <p>It is not thread safe, because it is called from single thread using a worker queue in {@link RecordingSession}.
 *
 * <p>Records screen frames and PCM audio into temporary raw files and exports them on demand.
 *
 * <p>Video is encoded through JCodec's AWT encoder. If PCM audio was captured alongside the
 * frames, the encoded video is remuxed with an uncompressed stereo audio track during export.
 */
@NotThreadSafe
public final class VideoRecorder {
    private static final Logger LOGGER = LoggerFactory.getLogger(VideoRecorder.class);
    private static final int AUDIO_BUFFER_SIZE = 16 * 1024;

    public static final String TEMP_VIDEO_FILE_PREFIX = "emustudio-zxspectrum-ula-video-";
    public static final String TEMP_AUDIO_FILE_PREFIX = "emustudio-zxspectrum-ula-audio-";
    public static final String TEMP_EXPORT_OUTPUT_FILE_PREFIX = "emustudio-zxspectrum-ula-video-export-";
    public static final String TEMP_EXPORT_VIDEO_ONLY_FILE_PREFIX = "emustudio-zxspectrum-ula-video-only-";


    private final Path tempVideoFile;
    private final OutputStream tempVideoFileOutputStream;
    private final Path tempAudioFile;
    private final OutputStream tempAudioFileOutputStream;

    private final int width;
    private final int height;

    private final int videoScale;
    private final int videoRate;
    private final int audioSampleRate;

    private final int frameByteSize;

    private final byte[] frameBuffer;
    private final IntBuffer frameIntBuffer; // mutable temporary buffer wrapping the frameBuffer

    private long videoFramesWritten;
    private long audioBytesWritten;

    private RECORDING_STATE recordingState = RECORDING_STATE.RUNNING;

    private enum RECORDING_STATE {
        RUNNING, STOPPED, ABORTED
    }

    /**
     * Creates new Video recorder.
     *
     * @param width           video width (in pixels)
     * @param height          video height (in pixels)
     * @param videoScale      denominator of the rational frame rate (fps = videoRate / videoScale)
     * @param videoRate       numerator of the rational frame rate (fps = videoRate / videoScale)
     * @param audioSampleRate Audio sample rate (samples/second)
     * @throws IOException when there is error during temp file creation
     */
    public VideoRecorder(int width, int height, int videoScale, int videoRate, int audioSampleRate) throws IOException {
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
        this.frameByteSize = width * height * Integer.BYTES;
        this.frameBuffer = new byte[frameByteSize];
        this.frameIntBuffer = ByteBuffer.wrap(frameBuffer).asIntBuffer();

        this.tempVideoFile = Files.createTempFile(TEMP_VIDEO_FILE_PREFIX, ".rgb");
        this.tempVideoFileOutputStream = new BufferedOutputStream(Files.newOutputStream(tempVideoFile));
        this.tempAudioFile = Files.createTempFile(TEMP_AUDIO_FILE_PREFIX, ".pcm");
        this.tempAudioFileOutputStream = new BufferedOutputStream(Files.newOutputStream(tempAudioFile));
    }

    /**
     * Captures (records) one video frame (keyframe)
     *
     * @param frame video frame (one picture)
     */
    public void captureVideo(BufferedImage frame) {
        if (recordingState == RECORDING_STATE.RUNNING) {
            try {
                if (frame.getWidth() != width || frame.getHeight() != height) {
                    throw new IOException("Recording frame size changed during capture");
                }

                // frame is always BufferedImage.TYPE_INT_RGB (see DisplayCanvas.captureFrame())
                if (frame.getType() == BufferedImage.TYPE_INT_RGB && frame.getRaster().getDataBuffer() instanceof DataBufferInt) {
                    DataBufferInt dataBuffer = (DataBufferInt) frame.getRaster().getDataBuffer();
                    int[] source = dataBuffer.getData();
                    frameIntBuffer.clear();
                    frameIntBuffer.put(source, 0, source.length);
                }

                tempVideoFileOutputStream.write(frameBuffer);
                videoFramesWritten++;
            } catch (IOException e) {
                LOGGER.error("Failed to write recording video frame; aborting recording", e);
                closeRecording(true);
            }
        }
    }

    /**
     * Captures (records) multiple audio frames
     *
     * <p>One audio frame has Beeper.CHANNELS, each channel contains an audio "sample" of has Beeper.BYTES_PER_SAMPLE size.
     * This audio frame size is stored in Beeper.FRAME_SIZE = Beeper.CHANNELS * Beeper.BYTES_PER_SAMPLE.
     *
     * @param pcmFrames array of PCM audio frames
     */
    public void captureAudio(byte[] pcmFrames) {
        Objects.requireNonNull(pcmFrames);
        if (recordingState == RECORDING_STATE.RUNNING) {
            if ((pcmFrames.length % Beeper.FRAME_SIZE) != 0) {
                LOGGER.error("Recording audio must contain whole PCM frames (expected frame size={}; last frame was={})",
                        Beeper.FRAME_SIZE, pcmFrames.length % Beeper.FRAME_SIZE);
                closeRecording(true);
            } else {
                try {
                    tempAudioFileOutputStream.write(pcmFrames);
                    audioBytesWritten += pcmFrames.length;
                } catch (IOException e) {
                    LOGGER.error("Failed to write recording audio data; aborting recording", e);
                    closeRecording(true);
                }
            }
        }
    }

    /**
     * Stops the recording.
     *
     * @param target Target path - if it's empty, recording will be discarded. If it's present, the video+audio will be
     *               saved to the given path. Video format will be guessed from the file extension.
     * @throws IOException when video contains no frames; could not create temp files; or unexpected error during encoding
     */
    public void stop(Path target) throws IOException {
        boolean wasAborted = recordingState == RECORDING_STATE.ABORTED;
        closeRecording(target == null);
        if (wasAborted) {
            throw new IOException("Recording was discarded after an earlier I/O error");
        }
        if (target != null) {
            try {
                exportVideo(target);
            } finally {
                deleteTempFiles();
            }
        }
    }

    /**
     * Closes capturing (i.e. clears buffers, closes output streams and deletes temp files if discard = true)
     *
     * @param discard whether to discard the recording (true will delete temp files)
     */
    private void closeRecording(boolean discard) {
        if (recordingState == RECORDING_STATE.RUNNING) {
            try {
                tempVideoFileOutputStream.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close recording video output stream", e);
            }
            try {
                tempAudioFileOutputStream.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close recording audio output stream", e);
            }
            recordingState = discard ? RECORDING_STATE.ABORTED : RECORDING_STATE.STOPPED;
        }

        if (discard) {
            deleteTempFiles();
        }
    }

    private void deleteTempFiles() {
        try {
            Files.deleteIfExists(tempVideoFile);
        } catch (IOException e) {
            LOGGER.error("Failed to delete recording temporary video file", e);
        }
        try {
            Files.deleteIfExists(tempAudioFile);
        } catch (IOException e) {
            LOGGER.error("Failed to delete recording temporary audio file", e);
        }
    }

    private void exportVideo(Path target) throws IOException {
        if (videoFramesWritten == 0) {
            throw new IOException("Recording contains no video frames");
        }

        Path tempOutputFile = Files.createTempFile(TEMP_EXPORT_OUTPUT_FILE_PREFIX, ".mp4");
        try {
            if (audioBytesWritten == 0) {
                exportVideoOnly(tempOutputFile);
            } else {
                Path tempVideo = Files.createTempFile(TEMP_EXPORT_VIDEO_ONLY_FILE_PREFIX, ".mp4");
                try {
                    exportVideoOnly(tempVideo);
                    muxAudio(tempVideo, tempOutputFile);
                } finally {
                    Files.deleteIfExists(tempVideo);
                }
            }
            moveFile(tempOutputFile, target);
        } catch (IOException e) {
            Files.deleteIfExists(tempOutputFile);
            throw e;
        }
    }

    private void exportVideoOnly(Path target) throws IOException {
        try (FileChannelWrapper channel = NIOUtils.writableChannel(target.toFile());
             InputStream frameInput = new BufferedInputStream(Files.newInputStream(tempVideoFile))) {
            AWTSequenceEncoder encoder = new AWTSequenceEncoder(channel, Rational.R(videoRate, videoScale));
            BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            int[] framePixels = ((DataBufferInt) frame.getRaster().getDataBuffer()).getData();

            for (long frameIndex = 0; frameIndex < videoFramesWritten; frameIndex++) {
                readFully(frameInput, frameBuffer, frameByteSize);
                frameIntBuffer.clear();
                frameIntBuffer.get(framePixels, 0, framePixels.length);
                encoder.encodeImage(frame);
            }

            encoder.finish();
        }
    }

    private void muxAudio(Path videoFile, Path outputFile) throws IOException {
        try (FileChannelWrapper inputChannel = NIOUtils.readableChannel(videoFile.toFile());
             MP4Demuxer demuxer = MP4Demuxer.createMP4Demuxer(inputChannel);
             FileChannelWrapper outputChannel = NIOUtils.writableChannel(outputFile.toFile())) {
            MP4DemuxerTrack videoTrack = (MP4DemuxerTrack) demuxer.getVideoTrack();
            if (videoTrack == null) {
                throw new IOException("Encoded recording is missing a video track");
            }

            MP4Muxer muxer = MP4Muxer.createMP4MuxerToChannel(outputChannel);
            copyVideoTrack(videoTrack, muxer);
            addAudioTrack(muxer);
            muxer.finish();
        }
    }

    private static void copyVideoTrack(MP4DemuxerTrack sourceTrack, MP4Muxer muxer) throws IOException {
        DemuxerTrackMeta meta = sourceTrack.getMeta();
        if (meta.getCodec() == null) {
            throw new IOException("Unable to determine the recorded video codec");
        }

        CodecMP4MuxerTrack videoTrack = (CodecMP4MuxerTrack) muxer.addVideoTrack(meta.getCodec(), meta.getVideoCodecMeta());
        MP4Packet packet;
        while ((packet = sourceTrack.nextFrame()) != null) {
            videoTrack.addFrame(packet);
        }
    }

    private void addAudioTrack(MP4Muxer muxer) throws IOException {
        if (audioBytesWritten == 0) {
            return;
        }
        if ((audioBytesWritten % Beeper.FRAME_SIZE) != 0) {
            throw new IOException("Temporary recording audio does not end on a PCM frame boundary");
        }

        PCMMP4MuxerTrack audioTrack = muxer.addPCMAudioTrack(new AudioFormat(audioSampleRate, Short.SIZE, Beeper.CHANNELS, true, false));
        writeAudioSamples(audioTrack);
    }

    private void writeAudioSamples(PCMMP4MuxerTrack audioTrack) throws IOException {
        byte[] buffer = new byte[AUDIO_BUFFER_SIZE];
        int pending = 0;

        try (InputStream audioInput = new BufferedInputStream(Files.newInputStream(tempAudioFile))) {
            while (true) {
                int read = audioInput.read(buffer, pending, buffer.length - pending);
                if (read < 0) {
                    break;
                }
                int total = pending + read;
                int complete = total - (total % Beeper.FRAME_SIZE);
                if (complete > 0) {
                    audioTrack.addSamples(ByteBuffer.wrap(Arrays.copyOf(buffer, complete)));
                }
                pending = total - complete;
                if (pending > 0) {
                    System.arraycopy(buffer, complete, buffer, 0, pending);
                }
            }
        }

        if (pending != 0) {
            throw new IOException("Temporary recording audio ended with an incomplete PCM frame");
        }
    }

    private static void readFully(InputStream input, byte[] buffer, int length) throws IOException {
        int offset = 0;
        while (offset < length) {
            int read = input.read(buffer, offset, length - offset);
            if (read < 0) {
                throw new IOException("Unexpected end of temporary recording data");
            }
            offset += read;
        }
    }

    private static void moveFile(Path source, Path target) throws IOException {
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(source);
        }
    }
}
