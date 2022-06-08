package net.emustudio.plugins.device.mits88dcdd.cmdline;

import net.emustudio.plugins.device.mits88dcdd.Resources;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
    name = "88-dcdd", mixinStandardHelpOptions = true, versionProvider = Runner.VersionProvider.class,
    description = "88-DCDD Altair floppy disk drive"
)
public class Runner implements Callable<Integer> {


    @Override
    public Integer call() throws Exception {
        return null;
    }

    public static class VersionProvider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() {
            return new String[]{Resources.getVersion()};
        }
    }
}
