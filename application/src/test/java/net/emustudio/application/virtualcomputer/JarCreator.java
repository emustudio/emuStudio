/*
 * Run-time library for emuStudio and plugins.
 *
 *     Copyright (C) 2006-2020  Peter Jakubčo
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.virtualcomputer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class JarCreator {

    public void createJar(File target, File classFile, List<String> dependencies) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(target))) {

            System.out.println("Creating JAR file, class=" + classFile);
            createManifest(zos, dependencies);

            ZipEntry zipEntry = new ZipEntry(
                    classFile.getParentFile().getName() + File.separator + classFile.getName()
            );

            System.out.println("JAR entry: " + zipEntry);
            zos.putNextEntry(zipEntry);
            zos.write(Files.readAllBytes(classFile.toPath()));
            zos.closeEntry();
        }
        System.out.println();
    }

    private void createManifest(ZipOutputStream zos, List<String> dependencies) throws IOException {
        ZipEntry zipEntry = new ZipEntry("META-INF/MANIFEST.MF");
        zos.putNextEntry(zipEntry);

        String manifestContent = "Manifest-Version: 1.0\n"
                + "Implementation-Title: Dummy plugin\n"
                + "Implementation-Version: 1.0.0\n"
                + "Implementation-Vendor-Id: net.sf.net.emustudio\n"
                + "Built-By: vbmacher\n"
                + "Build-Jdk: 1.8.0_45\n"
                + "Specification-Title: Dummy plugin\n"
                + "Specification-Version: 1.0.0\n"
                + "Archiver-Version: Plexus Archiver\n"
                + "Class-Path: ";

        StringBuilder classPath = new StringBuilder();
        for (String dep : dependencies) {
            classPath.append(dep).append(" ");
        }
        manifestContent += classPath.toString().concat("\n");

        System.out.println("Class-Path: " + classPath.toString());

        zos.write(manifestContent.getBytes());
        zos.closeEntry();
    }

}
