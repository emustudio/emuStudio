package net.emustudio.plugins.device.simh.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static net.emustudio.plugins.device.simh.CpmUtils.cpmCommandLine;
import static net.emustudio.plugins.device.simh.CpmUtils.createCPMCommandLine;

public class GetHostFilenames implements Command {
    public final static GetHostFilenames INS = new GetHostFilenames();

    private final static Logger LOGGER = LoggerFactory.getLogger(GetHostFilenames.class);

    // support for wild card file expansion
    public final static char hostPathSeparator = File.separatorChar;
    private final static char hostPathSeparatorAlt = File.separatorChar;

    private NameNode nameListHead;
    private NameNode currentName;
    private int currentNameIndex = 0;
    private int lastPathSeparatorIndex = 0;
    private int firstPathCharacterIndex = 0;


    static class NameNode {
        char[] name;
        NameNode next;

        public NameNode(char[] name, NameNode next) {
            this.name = name;
            this.next = next;
        }
    }

    @Override
    public void reset() {
        deleteNameList();
        nameListHead = null;
    }

    @Override
    public byte read(Control control) {
        byte result = 0;
        if (nameListHead != null) {
            if (currentName == null) {
                deleteNameList();
                control.clearCommand();
            } else if (firstPathCharacterIndex <= lastPathSeparatorIndex)
                result = (byte) cpmCommandLine[firstPathCharacterIndex++];
            else {
                result = (byte) currentName.name[currentNameIndex];
                if (result == 0) {
                    currentName = currentName.next;
                    firstPathCharacterIndex = currentNameIndex = 0;
                } else
                    currentNameIndex++;
            }
        }
        return result;
    }

    @Override
    public void write(byte data, Control control) {

    }

    @Override
    public void start(Control control) {
        if (nameListHead == null) {
            createCPMCommandLine(control.getMemory());
            lastPathSeparatorIndex = 0;
            while (cpmCommandLine[lastPathSeparatorIndex] != 0)
                lastPathSeparatorIndex++;
            while ((lastPathSeparatorIndex >= 0) &&
                (cpmCommandLine[lastPathSeparatorIndex] != hostPathSeparator) &&
                (cpmCommandLine[lastPathSeparatorIndex] != hostPathSeparatorAlt)) {
                lastPathSeparatorIndex--;
            }
            firstPathCharacterIndex = 0;
            deleteNameList();
            fillupNameList();
            currentName = nameListHead;
            currentNameIndex = 0;
        }
    }

    private void deleteNameList() {
        while (nameListHead != null) {
            nameListHead = nameListHead.next;
        }
        currentName = null;
        currentNameIndex = 0;
    }

    private void fillupNameList() {
        StringBuilder pb = new StringBuilder();
        for (char c : cpmCommandLine) {
            if (c != 0) {
                pb.append(c);
            } else {
                break;
            }
        }
        String path = pb.toString();
        try (Stream<Path> paths = Files.list(Path.of(path))) {
            paths.forEach(p -> nameListHead = new NameNode(p.getFileName().toString().toCharArray(), nameListHead));
        } catch (IOException e) {
            LOGGER.error("SIMH: Could not list host files", e);
            deleteNameList();
        }
    }
}
