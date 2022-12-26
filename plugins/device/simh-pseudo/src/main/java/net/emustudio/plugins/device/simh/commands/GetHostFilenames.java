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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static net.emustudio.plugins.device.simh.CpmUtils.cpmCommandLine;
import static net.emustudio.plugins.device.simh.CpmUtils.readCPMCommandLine;

public class GetHostFilenames implements Command {
    public final static GetHostFilenames INS = new GetHostFilenames();
    // support for wild card file expansion
    public final static char hostPathSeparator = File.separatorChar;
    public final static String hostPathSeparatorStr = String.valueOf(hostPathSeparator);
    private final static Logger LOGGER = LoggerFactory.getLogger(GetHostFilenames.class);
    private NameNode nameListHead;
    private NameNode currentName;
    private int currentNameIndex = 0;
    private int lastPathSeparatorIndex = 0;
    private int firstPathCharacterIndex = 0;

    @Override
    public void reset(Control control) {
        deleteNameList();
    }

    @Override
    public byte read(Control control) {
        byte result = 0;
        if (nameListHead != null) {
            if (currentName == null) {
                deleteNameList();
                control.clearCommand();
            } else if (firstPathCharacterIndex <= lastPathSeparatorIndex) {
                result = (byte) cpmCommandLine[firstPathCharacterIndex++];
            } else {
                if (currentNameIndex == currentName.name.length) {
                    currentName = currentName.next;
                    firstPathCharacterIndex = currentNameIndex = 0;
                    return 0;
                }
                result = (byte) currentName.name[currentNameIndex];
                currentNameIndex++;
            }
        }
        return result;
    }

    @Override
    public void start(Control control) {
        if (nameListHead == null) {
            readCPMCommandLine(control.getMemory());
            lastPathSeparatorIndex = 0;
            while (cpmCommandLine[lastPathSeparatorIndex] != 0) {
                lastPathSeparatorIndex++;
            }
            while ((lastPathSeparatorIndex >= 0) && (cpmCommandLine[lastPathSeparatorIndex] != hostPathSeparator)) {
                lastPathSeparatorIndex--;
            }
            firstPathCharacterIndex = 0;
            deleteNameList();
            fillupNameList();
            currentName = nameListHead;
            currentNameIndex = 0;
        }
        control.clearWriteCommand();
    }

    private void deleteNameList() {
        nameListHead = null;
        currentName = null;
        currentNameIndex = 0;
    }

    private void fillupNameList() {
        String cmdLine = cpmCmdLineToString();
        if (cmdLine.endsWith(hostPathSeparatorStr)) {
            nameListHead = new NameNode(new char[]{}, nameListHead); // simulating exact simh behavior
        } else {
            String[] parts = cmdLine.split(hostPathSeparatorStr); // all path parts must be moved to basePath

            StringBuilder basePathPostfixBuilder = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                basePathPostfixBuilder.append(parts[i]).append(hostPathSeparator);
            }
            Path basePath = Paths.get(System.getProperty("user.dir"));
            basePath = basePath.resolve(basePathPostfixBuilder.toString());
            String pattern = parts[parts.length - 1];

            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(basePath, pattern)) {
                dirStream.forEach(p -> {
                    nameListHead = new NameNode(p.getFileName().toString().toCharArray(), nameListHead);
                });
            } catch (IOException e) {
                LOGGER.error("SIMH: Could not list host files", e);
                deleteNameList();
            }
        }
    }

    private String cpmCmdLineToString() {
        StringBuilder pb = new StringBuilder();
        for (char c : cpmCommandLine) {
            if (c != 0) {
                pb.append(c);
            } else {
                break;
            }
        }
        return pb.toString();
    }

    static class NameNode {
        char[] name;
        NameNode next;

        public NameNode(char[] name, NameNode next) {
            this.name = name;
            this.next = next;
        }
    }
}
