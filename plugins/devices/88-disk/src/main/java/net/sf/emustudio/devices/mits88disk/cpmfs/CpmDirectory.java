package net.sf.emustudio.devices.mits88disk.cpmfs;

import net.jcip.annotations.NotThreadSafe;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@NotThreadSafe
public class CpmDirectory {
    public final static int DIRECTORY_ENTRY_SIZE = 32;
    public final static int DIRECTORY_TRACK = 6;

    private final List<CpmFile> files = new ArrayList<>();

    public CpmDirectory(List<CpmFile> files) {
        this.files.addAll(files);
    }

    public List<CpmFile> getAllFiles() {
        return Collections.unmodifiableList(files);
    }

    public List<CpmFile> filterValidFiles() {
        List<CpmFile> tmpFiles = new ArrayList<>();

        for (CpmFile file : files) {
            if (file.status < 32) {
                tmpFiles.add(file);
            }
        }
        return tmpFiles;
    }

    public String findDiscLabel() {
        for (CpmFile file : files) {
            if (file.status == 32) {
                return file.fileName + file.fileExt;
            }
        }
        return "";
    }

    private static List<ByteBuffer> getEntries(List<ByteBuffer> directorySectors) {
        List<ByteBuffer> entries = new ArrayList<>();

        for (ByteBuffer sector : directorySectors) {
            sector.position(3);
            int numberOfEntries = sector.remaining() / DIRECTORY_ENTRY_SIZE;

            for (int i = 0; i < numberOfEntries; i++) {
                byte[] entry = new byte[DIRECTORY_ENTRY_SIZE];
                sector.get(entry);

                entries.add(ByteBuffer.wrap(entry).asReadOnlyBuffer());
            }
        }
        return entries;
    }

    private static List<CpmFile> getFilesFromEntries(List<ByteBuffer> entries) {
        List<CpmFile> files = new ArrayList<>();

        for (ByteBuffer entry : entries) {
            files.add(CpmFile.fromEntry(entry));
        }

        return files;
    }

    public static CpmDirectory fromDisc(RawDisc disc) throws IOException {
        disc.reset(DIRECTORY_TRACK);
        List<ByteBuffer> directorySectors = disc.readBlock();
        List<ByteBuffer> entries = getEntries(directorySectors);
        List<CpmFile> files = getFilesFromEntries(entries);

        return new CpmDirectory(files);
    }

}
