package net.emustudio.plugins.device.mits88disk.cpmfs.commands;

import net.emustudio.plugins.device.mits88disk.cpmfs.CpmFileSystem;

import java.io.IOException;

public class InfoSubCommand implements CpmfsCommand.CpmfsSubCommand {

    @Override
    public void execute(CpmFileSystem fileSystem) throws IOException {
        System.out.println("Disc label: " + fileSystem.getLabel());
        System.out.println("Number of files: " + fileSystem.listValidFiles().size());
    }
}
