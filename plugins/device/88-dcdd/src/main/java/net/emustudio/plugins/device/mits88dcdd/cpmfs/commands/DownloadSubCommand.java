package net.emustudio.plugins.device.mits88dcdd.cpmfs.commands;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFileSystem;
import org.kohsuke.args4j.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class DownloadSubCommand implements CpmfsCommand.CpmfsSubCommand {
    private final static Logger LOGGER = LoggerFactory.getLogger(DownloadSubCommand.class);

    @Argument(required = true, metaVar = "source file name (in the disk image)", index = 0)
    String srcFileName;

    @Argument(metaVar = "destination file name (on host) - use \".\" (dot) to use the same name", index = 1)
    String dstFileName = ".";

    @Override
    public void execute(CpmFileSystem fileSystem) throws IOException {
        fileSystem.readContent(srcFileName).ifPresent(content -> {
            String realDstFileName = dstFileName.equals(".") ? srcFileName : dstFileName;
            try (Writer writer = new FileWriter(realDstFileName)) {
                writer.write(content);
            } catch (IOException e) {
                LOGGER.error("Could not write file content", e);
            }
        });
    }
}
