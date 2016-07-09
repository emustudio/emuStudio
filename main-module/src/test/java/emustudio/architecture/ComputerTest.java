package emustudio.architecture;

import emulib.emustudio.SettingsManager;
import emulib.runtime.exceptions.PluginInitializationException;
import org.junit.Test;

import java.util.Collections;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ComputerTest {

    @Test(expected = NullPointerException.class)
    public void testNewComputerWithNullNameThrows() throws Exception {
        new Computer(null, Collections.emptyList(), Collections.emptyMap());
    }

    @Test(expected = NullPointerException.class)
    public void testNewComputerWithNullPluginInfosThrows() throws Exception {
        new Computer("name", null, Collections.emptyMap());
    }

    @Test(expected = NullPointerException.class)
    public void testNewComputerWithNullConnectionsThrows() throws Exception {
        new Computer("name", Collections.emptyList(), null);
    }

    @Test
    public void testNewComputerNoPluginsStillWork() throws Exception {
        Computer computer = new Computer("name", Collections.emptyList(), Collections.emptyMap());

        assertFalse(computer.getCompiler().isPresent());
        assertFalse(computer.getCPU().isPresent());
        assertFalse(computer.getMemory().isPresent());
        assertEquals(0, computer.getDeviceCount());
    }

    @Test(expected = PluginInitializationException.class)
    public void testInitializeThrowsIfCPUisNotSet() throws Exception {
        Computer computer = new Computer("name", Collections.emptyList(), Collections.emptyMap());

        computer.initialize(createMock(SettingsManager.class));
    }
}
