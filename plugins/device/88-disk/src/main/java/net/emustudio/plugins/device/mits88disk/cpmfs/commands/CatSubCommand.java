package net.emustudio.plugins.device.mits88disk.cpmfs.commands;

import net.emustudio.plugins.device.mits88disk.cpmfs.CpmFileSystem;
import org.kohsuke.args4j.Argument;

import java.io.IOException;

public class CatSubCommand implements CpmfsCommand.CpmfsSubCommand {
    @Argument(required = true, metaVar = "file name")
    String fileName;

    @Override
    public void execute(CpmFileSystem fileSystem) throws IOException {
        fileSystem.readContent(fileName).ifPresent(System.out::println);
    }
}
