package net.emustudio.plugins.device.mits88dcdd.cpmfs.commands;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFileSystem;

import java.io.IOException;

public class DatesSubCommand implements CpmfsCommand.CpmfsSubCommand {
    @Override
    public void execute(CpmFileSystem fileSystem) throws IOException {
        System.out.printf("%12s | %16s | %16s | %16s%n", "File name", "Create", "Modify", "Access");
        System.out.println("-------------+------------------+------------------+-----------------");
        fileSystem.listNativeDates().forEach(System.out::println);
    }
}
