package net.emustudio.plugins.device.mits88dcdd.cpmfs.commands;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFileSystem;
import org.kohsuke.args4j.Argument;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

public class UploadSubCommand implements CpmfsCommand.CpmfsSubCommand {
    @Argument(required = true, metaVar = "source file name (on host)")
    String srcFileName;

    @Argument(metaVar = "destination file name (in the disk image) - use \".\" (dot) to use the same name", index = 1)
    String dstFileName = ".";

    @Override
    public void execute(CpmFileSystem fileSystem) throws IOException {
        StringWriter content = new StringWriter();
        try (Reader reader = new FileReader(srcFileName)) {
            reader.transferTo(content);
        }
        String realDstFileName = (dstFileName.equals(".")) ? srcFileName : dstFileName;
        fileSystem.writeFile(realDstFileName, content.toString());
    }
}
