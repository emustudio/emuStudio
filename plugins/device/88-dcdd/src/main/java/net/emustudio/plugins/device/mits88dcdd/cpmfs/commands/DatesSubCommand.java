package net.emustudio.plugins.device.mits88dcdd.cpmfs.commands;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFileSystem;

import java.io.IOException;

public class DatesSubCommand implements CpmfsCommand.CpmfsSubCommand {
    @Override
    public void execute(CpmFileSystem fileSystem) throws IOException {
        fileSystem.listNativeDates().forEach(System.out::println);
    }
}
