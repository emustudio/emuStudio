/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * One-time usable Video+Audio recorder.
 *
 * <p>It is not thread safe, because it is called from single thread using a worker queue in {@link RecordingSession}.
 *
 * <p>Encodes screen frames during capture and stores PCM audio in a compressed temporary file.
 *
 * <p>Video is encoded through JCodec's AWT encoder. If PCM audio was captured alongside the
 * frames, the encoded video is remuxed with an uncompressed stereo audio track during export.
 */
final class VideoRecorder {
    private static final Logger LOGGER = LoggerFactory.getLogger(VideoRecorder.class);
    private static final int AUDIO_BUFFER_SIZE = 16 * 1024;
    private static final int COMPRESSION_BUFFER_SIZE = 64 * 1024;
    private static final int AUDIO_CHANNELS = 2;
    private static final int AUDIO_FRAME_SIZE = AUDIO_CHANNELS * Short.BYTES;

    public static final String TEMP_VIDEO_FILE_PREFIX = "emustudio-space-invaders-video-";
    public static final String TEMP_AUDIO_FILE_PREFIX = "emustudio-space-invaders-audio-";
    public static final String TEMP_EXPORT_OUTPUT_FILE_PREFIX = "emustudio-space-invaders-video-export-";


    private final Path tempVideoFile;
    private final FileChannelWrapper tempVideoChannel;
    private final AWTSequenceEncoder encoder;
    private final Path tempAudioFile;
    private final OutputStream tempAudioFileOutputStream;

    private final int width;
    private final int height;

    private final int audioSampleRate;

    private long videoFramesWritten;
    private long audioBytesWritten;

    private RECORDING_STATE recordingState = RECORDING_STATE.RUNNING;
    private volatile IOException captureFailure;

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

        this.audioSampleRate = audioSampleRate;
        this.tempVideoFile = Files.createTempFile(TEMP_VIDEO_FILE_PREFIX, ".mp4");
        FileChannelWrapper channel = null;
        OutputStream audioOutput = null;
        Path audioFile = null;
        try {
            channel = NIOUtils.writableChannel(tempVideoFile.toFile());
            this.encoder = new AWTSequenceEncoder(channel, Rational.R(videoRate, videoScale));
            audioFile = Files.createTempFile(TEMP_AUDIO_FILE_PREFIX, ".pcm.gz");
            audioOutput = new BufferedOutputStream(Files.newOutputStream(audioFile));
            this.tempAudioFileOutputStream = new GZIPOutputStream(
                    audioOutput, COMPRESSION_BUFFER_SIZE);
            this.tempAudioFile = audioFile;
            this.tempVideoChannel = channel;
        } catch (IOException | RuntimeException e) {
            NIOUtils.closeQuietly(channel);
            NIOUtils.closeQuietly(audioOutput);
            Files.deleteIfExists(tempVideoFile);
            if (audioFile != null) {
                Files.deleteIfExists(audioFile);
            }
            throw e;
        }
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

                encoder.encodeImage(frame);
                videoFramesWritten++;
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Failed to write recording video frame; aborting recording", e);
                captureFailure = new IOException("Could not encode recording video", e);
                closeRecording(true);
            }
        }
    }

    /**
     * Captures (records) multiple audio frames
     *
     * <p>One audio frame has AUDIO_CHANNELS, each channel contains an audio "sample" of has Short.BYTES size.
     * This audio frame size is stored in AUDIO_FRAME_SIZE = AUDIO_CHANNELS * Short.BYTES.
     *
     * @param pcmFrames array of PCM audio frames
     */
    public void captureAudio(byte[] pcmFrames) {
        Objects.requireNonNull(pcmFrames);
        if (recordingState == RECORDING_STATE.RUNNING) {
            if ((pcmFrames.length % AUDIO_FRAME_SIZE) != 0) {
                LOGGER.error("Recording audio must contain whole PCM frames (expected frame size={}; last frame was={})",
                        AUDIO_FRAME_SIZE, pcmFrames.length % AUDIO_FRAME_SIZE);
                captureFailure = new IOException("Recording audio contains an incomplete PCM frame");
                closeRecording(true);
            } else {
                try {
                    tempAudioFileOutputStream.write(pcmFrames);
                    audioBytesWritten += pcmFrames.length;
                } catch (IOException e) {
                    LOGGER.error("Failed to write recording audio data; aborting recording", e);
                    captureFailure = e;
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
        closeRecording(target == null);
        if (captureFailure != null && target != null) {
            deleteTempFiles();
            throw captureFailure;
        }
        if (target != null) {
            try {
                exportVideo(target);
            } finally {
                deleteTempFiles();
            }
        }
    }

    IOException getCaptureFailure() {
        return captureFailure;
    }

    /**
     * Closes capturing (i.e. clears buffers, closes output streams and deletes temp files if discard = true)
     *
     * @param discard whether to discard the recording (true will delete temp files)
     */
    private void closeRecording(boolean discard) {
        if (recordingState == RECORDING_STATE.RUNNING) {
            if (!discard && videoFramesWritten > 0) {
                try {
                    encoder.finish();
                } catch (IOException | RuntimeException e) {
                    LOGGER.error("Failed to finalize recording video", e);
                    captureFailure = new IOException("Could not finalize recording video", e);
                }
            }
            try {
                tempVideoChannel.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close recording video output stream", e);
                captureFailure = e;
            }
            try {
                tempAudioFileOutputStream.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close recording audio output stream", e);
                captureFailure = e;
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
                Files.copy(tempVideoFile, tempOutputFile, StandardCopyOption.REPLACE_EXISTING);
            } else {
                muxAudio(tempVideoFile, tempOutputFile);
            }
            moveFile(tempOutputFile, target);
        } catch (IOException e) {
            Files.deleteIfExists(tempOutputFile);
            throw e;
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
        if ((audioBytesWritten % AUDIO_FRAME_SIZE) != 0) {
            throw new IOException("Temporary recording audio does not end on a PCM frame boundary");
        }

        PCMMP4MuxerTrack audioTrack = muxer.addPCMAudioTrack(new AudioFormat(audioSampleRate, Short.SIZE, AUDIO_CHANNELS, true, false));
        writeAudioSamples(audioTrack);
    }

    private void writeAudioSamples(PCMMP4MuxerTrack audioTrack) throws IOException {
        byte[] buffer = new byte[AUDIO_BUFFER_SIZE];
        int pending = 0;

        try (InputStream audioInput = new GZIPInputStream(
                new BufferedInputStream(Files.newInputStream(tempAudioFile)), COMPRESSION_BUFFER_SIZE)) {
            while (true) {
                int read = audioInput.read(buffer, pending, buffer.length - pending);
                if (read < 0) {
                    break;
                }
                int total = pending + read;
                int complete = total - (total % AUDIO_FRAME_SIZE);
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
